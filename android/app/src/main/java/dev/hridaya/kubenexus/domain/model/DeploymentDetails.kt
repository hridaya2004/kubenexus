package dev.hridaya.kubenexus.domain.model

/**
 * One entry of a Deployment's status.conditions, e.g. Available / Progressing /
 * ReplicaFailure.
 *
 * [lastUpdateMillis] is epoch millis rather than the RFC 3339 string the API
 * server sends, so screens can derive "x ago" labels that stay correct without
 * refetching — same reasoning as the pods cache moving to timestamps.
 */
data class DeploymentCondition(
    val type: String,
    val status: String,
    val lastUpdateMillis: Long?,
    val reason: String?,
    val message: String?,
)

/**
 * Full describe-view of an apps/v1 Deployment, the detail-screen counterpart of
 * the lean [DeploymentSummary].
 *
 * Field names are pinned by the deployment detail UI and must not be renamed;
 * add-only evolution keeps those call sites compiling.
 */
data class DeploymentDetails(
    val name: String,
    val namespace: String,
    val creationTimestampMillis: Long,
    val desiredReplicas: Int,
    val readyReplicas: Int,
    val availableReplicas: Int,
    val updatedReplicas: Int,

    /** spec.strategy.type ("RollingUpdate"/"Recreate"); null when defaulted by the cluster. */
    val strategyType: String?,

    /** spec.minReadySeconds; null when unset. */
    val minReadySeconds: Int?,

    /** spec.selector.matchLabels, shown verbatim since it is what pods are matched on. */
    val selectorMatchLabels: Map<String, String>,
    val labels: Map<String, String>,
    val annotations: Map<String, String>,
    val conditions: List<DeploymentCondition>,
    val images: List<String>,
    val events: List<K8sEventSummary>,
)
