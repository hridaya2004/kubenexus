package dev.hridaya.kubenexus.data.mapper

import dev.hridaya.kubenexus.core.common.time.K8sTime
import dev.hridaya.kubenexus.data.source.local.entity.DeploymentEntity
import dev.hridaya.kubenexus.data.source.remote.dto.DeploymentConditionDto
import dev.hridaya.kubenexus.data.source.remote.dto.DeploymentDto
import dev.hridaya.kubenexus.data.source.remote.dto.EventDto
import dev.hridaya.kubenexus.domain.model.DeploymentCondition
import dev.hridaya.kubenexus.domain.model.DeploymentDetails
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.model.K8sEventSummary

private const val DEFAULT_NAMESPACE = "default"

/**
 * Images are stored as one comma-joined TEXT column, mirroring how PodEntity
 * keeps flat columns instead of child tables or type converters. A comma can
 * never occur inside an image reference (registry/repository/tag/digest all
 * exclude it), so the join is losslessly reversible.
 */
private const val IMAGES_SEPARATOR = ","

/**
 * Maps an event to the workload-detail event shape. Kubernetes populates
 * whichever of lastTimestamp/eventTime/firstTimestamp the emitting component
 * uses; the latest of the three is the one worth showing.
 */
fun EventDto.toEventSummary(): K8sEventSummary = K8sEventSummary(
    type = type.ifBlank { null },
    reason = reason.ifBlank { null },
    message = message.ifBlank { null },
    count = count,
    lastTimestampMillis =
        K8sTime.parseTimestampMillis(lastTimestamp ?: eventTime ?: firstTimestamp),
)

/**
 * Builds the describe-view of a Deployment. [events] comes from a separate
 * field-selected API call and is passed in rather than fetched here so a
 * failure fetching events cannot lose the primary payload.
 */
fun DeploymentDto.toDeploymentDetails(events: List<EventDto> = emptyList()): DeploymentDetails {
    val namespace = metadata.namespace.ifBlank { DEFAULT_NAMESPACE }
    return DeploymentDetails(
        name = metadata.name,
        namespace = namespace,
        creationTimestampMillis = K8sTime.parseTimestampMillis(metadata.creationTimestamp) ?: 0L,
        desiredReplicas = spec.replicas,
        readyReplicas = status.readyReplicas,
        availableReplicas = status.availableReplicas,
        updatedReplicas = status.updatedReplicas,
        strategyType = spec.strategy.type?.ifBlank { null },
        minReadySeconds = spec.minReadySeconds,
        selectorMatchLabels = spec.selector.matchLabels,
        labels = metadata.labels,
        annotations = metadata.annotations,
        conditions = status.conditions.map { it.toDomain() },
        images = spec.template.spec.containers.map { it.image }.filter { it.isNotBlank() },
        events = events.map { it.toEventSummary() },
    )
}

private fun DeploymentConditionDto.toDomain(): DeploymentCondition = DeploymentCondition(
    type = type,
    status = status,
    lastUpdateMillis = K8sTime.parseTimestampMillis(lastUpdateTime),
    reason = reason?.ifBlank { null },
    message = message?.ifBlank { null },
)

/** Maps a Deployment to its cache row; see [IMAGES_SEPARATOR] for the images encoding. */
fun DeploymentSummary.toEntity(clusterId: String): DeploymentEntity = DeploymentEntity(
    id = "${clusterId}_${namespace}_$name",
    clusterId = clusterId,
    name = name,
    namespace = namespace,
    desiredReplicas = desiredReplicas,
    readyReplicas = readyReplicas,
    availableReplicas = availableReplicas,
    // List summaries don't carry the rollout's updated count; the column stays
    // so describe payloads can upgrade rows later without a migration.
    updatedReplicas = 0,
    creationTimestampMillis = creationTimestampMillis,
    images = images.joinToString(separator = IMAGES_SEPARATOR),
)

fun DeploymentEntity.toDomain(): DeploymentSummary = DeploymentSummary(
    id = id,
    name = name,
    namespace = namespace,
    desiredReplicas = desiredReplicas,
    readyReplicas = readyReplicas,
    availableReplicas = availableReplicas,
    images = images.split(IMAGES_SEPARATOR).filter { it.isNotBlank() },
    creationTimestampMillis = creationTimestampMillis ?: 0L,
)
