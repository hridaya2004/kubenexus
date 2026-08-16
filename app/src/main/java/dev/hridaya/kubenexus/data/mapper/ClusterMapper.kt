package dev.hridaya.kubenexus.data.mapper

import dev.hridaya.kubenexus.data.source.local.entity.ClusterEntity
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.model.ClusterStatus

fun ClusterEntity.toDomain(): Cluster {
    val clusterStatus = try {
        ClusterStatus.valueOf(status)
    } catch (_: Exception) {
        ClusterStatus.DISCONNECTED
    }

    return Cluster(
        id = id,
        name = name,
        serverUrl = serverUrl,
        rawKubeconfig = rawKubeconfig,
        contextName = contextName,
        userName = userName,
        namespace = namespace,
        isActive = isActive,
        createdAt = createdAt,
        lastConnectedAt = lastConnectedAt,
        status = clusterStatus
    )
}

fun Cluster.toEntity(): ClusterEntity {
    return ClusterEntity(
        id = id,
        name = name,
        serverUrl = serverUrl,
        rawKubeconfig = rawKubeconfig,
        contextName = contextName,
        userName = userName,
        namespace = namespace,
        isActive = isActive,
        createdAt = createdAt,
        lastConnectedAt = lastConnectedAt,
        status = status.name
    )
}
