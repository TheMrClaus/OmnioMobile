package com.nuvio.app.features.emby

/**
 * Pure decision logic for resume reconciliation between Emby's server-side
 * playback position and OmnioMobile's local WatchProgress entry.
 *
 * Emby's API does not expose a per-item "last updated" timestamp on UserData, so a
 * pure timestamp comparison is impossible. Both sides are monotonic during a single
 * playback (positions only grow until a session ends), so we approximate "most-recent
 * write wins" with: the larger position wins when both sides have one.
 *
 * Tie-break (positions equal): prefer LOCAL to avoid a surprise re-seek when the user
 * explicitly resumed locally and then hit play.
 *
 * Returning the chosen [ResumeSource] tells the caller which side to bring up-to-date
 * (e.g. write the chosen position back into local WatchProgress, or push it to Emby
 * via the next progress report).
 */
internal object EmbyResumeReconciler {

    enum class ResumeSource { LOCAL, EMBY, NONE }

    data class ResumeDecision(
        val positionMs: Long,
        val source: ResumeSource,
    ) {
        companion object {
            val NONE = ResumeDecision(positionMs = 0L, source = ResumeSource.NONE)
        }
    }

    fun reconcile(
        localPositionMs: Long?,
        embyPositionMs: Long?,
    ): ResumeDecision {
        val local = localPositionMs?.takeIf { it > 0L }
        val emby = embyPositionMs?.takeIf { it > 0L }
        return when {
            local == null && emby == null -> ResumeDecision.NONE
            local != null && emby == null -> ResumeDecision(local, ResumeSource.LOCAL)
            local == null && emby != null -> ResumeDecision(emby, ResumeSource.EMBY)
            else -> {
                if (emby!! > local!!) {
                    ResumeDecision(emby, ResumeSource.EMBY)
                } else {
                    ResumeDecision(local, ResumeSource.LOCAL)
                }
            }
        }
    }
}
