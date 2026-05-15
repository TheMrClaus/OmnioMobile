package com.nuvio.app.features.emby

import com.nuvio.app.features.emby.EmbyResumeReconciler.ResumeSource
import kotlin.test.Test
import kotlin.test.assertEquals

class EmbyResumeReconcilerTest {

    @Test
    fun `both null returns NONE`() {
        val decision = EmbyResumeReconciler.reconcile(null, null)
        assertEquals(ResumeSource.NONE, decision.source)
        assertEquals(0L, decision.positionMs)
    }

    @Test
    fun `both zero returns NONE`() {
        val decision = EmbyResumeReconciler.reconcile(0L, 0L)
        assertEquals(ResumeSource.NONE, decision.source)
        assertEquals(0L, decision.positionMs)
    }

    @Test
    fun `local present and emby null returns LOCAL`() {
        val decision = EmbyResumeReconciler.reconcile(45_000L, null)
        assertEquals(ResumeSource.LOCAL, decision.source)
        assertEquals(45_000L, decision.positionMs)
    }

    @Test
    fun `local null and emby present returns EMBY`() {
        val decision = EmbyResumeReconciler.reconcile(null, 60_000L)
        assertEquals(ResumeSource.EMBY, decision.source)
        assertEquals(60_000L, decision.positionMs)
    }

    @Test
    fun `local zero treated as missing when emby present`() {
        val decision = EmbyResumeReconciler.reconcile(0L, 30_000L)
        assertEquals(ResumeSource.EMBY, decision.source)
        assertEquals(30_000L, decision.positionMs)
    }

    @Test
    fun `emby zero treated as missing when local present`() {
        val decision = EmbyResumeReconciler.reconcile(30_000L, 0L)
        assertEquals(ResumeSource.LOCAL, decision.source)
        assertEquals(30_000L, decision.positionMs)
    }

    @Test
    fun `larger emby wins`() {
        val decision = EmbyResumeReconciler.reconcile(45_000L, 90_000L)
        assertEquals(ResumeSource.EMBY, decision.source)
        assertEquals(90_000L, decision.positionMs)
    }

    @Test
    fun `larger local wins`() {
        val decision = EmbyResumeReconciler.reconcile(120_000L, 60_000L)
        assertEquals(ResumeSource.LOCAL, decision.source)
        assertEquals(120_000L, decision.positionMs)
    }

    @Test
    fun `tie prefers LOCAL`() {
        val decision = EmbyResumeReconciler.reconcile(75_000L, 75_000L)
        assertEquals(ResumeSource.LOCAL, decision.source)
        assertEquals(75_000L, decision.positionMs)
    }

    @Test
    fun `negative local treated as missing`() {
        val decision = EmbyResumeReconciler.reconcile(-1L, 10_000L)
        assertEquals(ResumeSource.EMBY, decision.source)
        assertEquals(10_000L, decision.positionMs)
    }
}
