package dev.hridaya.kubenexus.domain.model

/**
 * Kubernetes event as surfaced on workload detail screens (Deployments,
 * Services), in the shape the events API actually returns.
 *
 * Distinct from [PodEventDetail], which carries a pre-rendered `age` string for
 * the pod screen. Workload details keep the raw [count] and
 * [lastTimestampMillis] instead so the UI can sort, aggregate and re-render ages
 * without a cache refresh freezing the value, mirroring why Pod rows moved from
 * a formatted age string to epoch millis.
 */
data class K8sEventSummary(
    /** "Normal" or "Warning"; null when the API server omitted it. */
    val type: String?,
    val reason: String?,
    val message: String?,

    /**
     * How many times this exact event fired. The API server coalesces repeats
     * into one object with an incremented count rather than emitting new rows.
     */
    val count: Int,

    /** Epoch millis of the most recent occurrence; null when unknown. */
    val lastTimestampMillis: Long?,
)
