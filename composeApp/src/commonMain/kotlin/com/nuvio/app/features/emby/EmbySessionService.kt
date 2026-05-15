package com.nuvio.app.features.emby

import co.touchlab.kermit.Logger
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reports playback session events to the Emby server alongside Trakt scrobbling.
 *
 * Lifecycle (called from PlayerScreen seams):
 *  - [reportStart] when playback begins for an Emby stream.
 *  - [reportProgress] every ~10s while playing (also on pause/resume with [force] = true).
 *  - [reportStop] when playback ends — runs on the service scope so the stop event fires
 *    even if the caller's scope has been cancelled.
 *
 * State is held per-instance so a new "session" per playback is created via [Uuid.random]
 * on each [reportStart].
 */
@OptIn(ExperimentalUuidApi::class)
class EmbySessionService {
    private val log = Logger.withTag("EmbySession")
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentItemId: String? = null
    private var currentMediaSourceId: String? = null
    private var currentPlaySessionId: String? = null
    private var hasReportedStart: Boolean = false
    private var lastProgressReportMs: Long = 0L

    private val progressIntervalMs = 10_000L

    suspend fun reportStart(itemId: String, mediaSourceId: String, positionMs: Long = 0L) {
        val payload = EmbyAuthRepository.currentPayload() ?: return
        if (hasReportedStart && currentItemId == itemId && currentMediaSourceId == mediaSourceId) return

        val playSessionId = Uuid.random().toString()
        try {
            val response = EmbyApiClient.reportPlaybackStart(
                payload = payload,
                dto = EmbyPlaybackStartDto(
                    itemId = itemId,
                    mediaSourceId = mediaSourceId,
                    playSessionId = playSessionId,
                    positionTicks = msToTicks(positionMs),
                ),
            )
            if (response.status in 200..299) {
                currentItemId = itemId
                currentMediaSourceId = mediaSourceId
                currentPlaySessionId = playSessionId
                hasReportedStart = true
                lastProgressReportMs = TraktPlatformClock.nowEpochMs()
            } else {
                log.w { "Failed to report playback start: ${response.status}" }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            log.w { "Error reporting playback start: ${error.message}" }
        }
    }

    suspend fun reportProgress(positionMs: Long, isPaused: Boolean = false, force: Boolean = false) {
        val payload = EmbyAuthRepository.currentPayload() ?: return
        val itemId = currentItemId ?: return
        val mediaSourceId = currentMediaSourceId ?: return
        val playSessionId = currentPlaySessionId ?: return
        if (!hasReportedStart) return

        val now = TraktPlatformClock.nowEpochMs()
        if (!force && now - lastProgressReportMs < progressIntervalMs) return

        try {
            val response = EmbyApiClient.reportPlaybackProgress(
                payload = payload,
                dto = EmbyPlaybackProgressDto(
                    itemId = itemId,
                    mediaSourceId = mediaSourceId,
                    playSessionId = playSessionId,
                    positionTicks = msToTicks(positionMs),
                    isPaused = isPaused,
                ),
            )
            if (response.status in 200..299) {
                lastProgressReportMs = now
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            log.w { "Error reporting Emby progress: ${error.message}" }
        }
    }

    /**
     * Best-effort stop. Runs on a service-owned scope so it survives the caller's
     * scope cancellation (e.g. when leaving the player screen).
     */
    fun reportStop(positionMs: Long = 0L) {
        val itemId = currentItemId
        val mediaSourceId = currentMediaSourceId
        val playSessionId = currentPlaySessionId
        val wasStarted = hasReportedStart
        resetSession()

        if (!wasStarted || itemId == null || mediaSourceId == null || playSessionId == null) return

        serviceScope.launch {
            val payload = EmbyAuthRepository.currentPayload() ?: return@launch
            try {
                EmbyApiClient.reportPlaybackStopped(
                    payload = payload,
                    dto = EmbyPlaybackStopDto(
                        itemId = itemId,
                        mediaSourceId = mediaSourceId,
                        playSessionId = playSessionId,
                        positionTicks = msToTicks(positionMs),
                    ),
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                log.w { "Error reporting Emby stop: ${error.message}" }
            }
        }
    }

    fun resetSession() {
        currentItemId = null
        currentMediaSourceId = null
        currentPlaySessionId = null
        hasReportedStart = false
        lastProgressReportMs = 0L
    }

    private fun msToTicks(ms: Long): Long = ms * 10_000L
}
