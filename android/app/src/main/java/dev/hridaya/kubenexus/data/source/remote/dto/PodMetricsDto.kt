package dev.hridaya.kubenexus.data.source.remote.dto

import dev.hridaya.kubenexus.core.common.util.QuantityParser
import dev.hridaya.kubenexus.core.common.time.K8sTime
import dev.hridaya.kubenexus.domain.model.PodMetricSample
import kotlinx.serialization.Serializable

@Serializable
data class ContainerUsageDto(
    val cpu: String = "",
    val memory: String = "",
)

@Serializable
data class MetricsContainerDto(
    val name: String = "",
    val usage: ContainerUsageDto = ContainerUsageDto(),
)

@Serializable
data class PodMetricsDto(
    val metadata: ObjectMetaDto = ObjectMetaDto(),
    val timestamp: String? = null,
    val window: String? = null,
    val containers: List<MetricsContainerDto> = emptyList(),
)

@Serializable
data class PodMetricsListDto(
    val items: List<PodMetricsDto> = emptyList(),
)

fun PodMetricsDto.toSample(): PodMetricSample? {
    if (metadata.name.isBlank()) return null
    var cpuCores = 0.0
    var memoryBytes = 0L
    containers.forEach { container ->
        cpuCores += QuantityParser.parseCores(container.usage.cpu)
        memoryBytes += QuantityParser.parseBytes(container.usage.memory)
    }
    return PodMetricSample(
        podName = metadata.name,
        timestampMillis = K8sTime.parseTimestampMillis(timestamp) ?: System.currentTimeMillis(),
        cpuCores = cpuCores,
        memoryBytes = memoryBytes,
    )
}
