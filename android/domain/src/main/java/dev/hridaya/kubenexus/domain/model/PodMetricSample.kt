package dev.hridaya.kubenexus.domain.model

data class PodMetricSample(
    val podName: String,
    val timestampMillis: Long,
    /** Summed CPU usage across the pod's containers, in cores. */
    val cpuCores: Double,
    /** Summed memory usage across the pod's containers, in bytes. */
    val memoryBytes: Long,
)
