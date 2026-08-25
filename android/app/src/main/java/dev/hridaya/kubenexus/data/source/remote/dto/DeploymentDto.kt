package dev.hridaya.kubenexus.data.source.remote.dto

import dev.hridaya.kubenexus.core.common.time.K8sTime
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import kotlinx.serialization.Serializable

private const val DEFAULT_NAMESPACE = "default"

@Serializable
data class DeploymentDto(
    val apiVersion: String? = null,
    val kind: String? = null,
    val metadata: ObjectMetaDto = ObjectMetaDto(),
    val spec: DeploymentSpecDto = DeploymentSpecDto(),
    val status: DeploymentStatusDto = DeploymentStatusDto(),
)

@Serializable
data class DeploymentListDto(
    val items: List<DeploymentDto> = emptyList(),
)

@Serializable
data class DeploymentSpecDto(
    // The API server defaults omitted replicas to 1; mirroring that here keeps
    // a defaulted Deployment from rendering as scaled to zero.
    val replicas: Int = 1,
    val template: DeploymentTemplateDto = DeploymentTemplateDto(),
)

@Serializable
data class DeploymentTemplateDto(
    val spec: DeploymentPodSpecDto = DeploymentPodSpecDto(),
)

@Serializable
data class DeploymentPodSpecDto(
    val containers: List<ContainerDto> = emptyList(),
)

@Serializable
data class DeploymentStatusDto(
    val replicas: Int = 0,
    val readyReplicas: Int = 0,
    val availableReplicas: Int = 0,
    val updatedReplicas: Int = 0,
)

fun DeploymentDto.toDomain(): DeploymentSummary {
    val namespace = metadata.namespace.ifBlank { DEFAULT_NAMESPACE }
    return DeploymentSummary(
        id = "${namespace}_${metadata.name}",
        name = metadata.name,
        namespace = namespace,
        desiredReplicas = spec.replicas,
        readyReplicas = status.readyReplicas,
        availableReplicas = status.availableReplicas,
        images = spec.template.spec.containers.map { it.image }.filter { it.isNotBlank() },
        creationTimestampMillis = K8sTime.parseTimestampMillis(metadata.creationTimestamp) ?: 0L,
    )
}
