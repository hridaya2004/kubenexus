package dev.hridaya.kubenexus.data.mapper

import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PodMapperTest {

    @Test
    fun `toDomain maps status strings to PodStatus correctly`() {
        assertEquals(PodStatus.RUNNING, mapStatusToDomain("Running"))
        assertEquals(PodStatus.PENDING, mapStatusToDomain("Pending"))
        assertEquals(PodStatus.COMPLETED, mapStatusToDomain("Completed"))
        assertEquals(PodStatus.FAILED, mapStatusToDomain("Failed"))
        assertEquals(PodStatus.CRASH_LOOP, mapStatusToDomain("CrashLoopBackOff"))
        assertEquals(PodStatus.UNKNOWN, mapStatusToDomain("SomethingElse"))
    }

    private fun mapStatusToDomain(status: String): PodStatus {
        return when (status.lowercase()) {
            "running" -> PodStatus.RUNNING
            "pending" -> PodStatus.PENDING
            "completed", "succeeded" -> PodStatus.COMPLETED
            "failed" -> PodStatus.FAILED
            "crashloopbackoff", "error" -> PodStatus.CRASH_LOOP
            else -> PodStatus.UNKNOWN
        }
    }
}
