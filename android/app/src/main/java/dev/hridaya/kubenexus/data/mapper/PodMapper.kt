package dev.hridaya.kubenexus.data.mapper

import client.ContainerInfo as NativeContainerInfo
import client.Namespace as NativeNamespace
import client.Pod as NativePod
import client.PodCondition as NativePodCondition
import client.PodDetails as NativePodDetails
import client.PodEvent as NativePodEvent
import dev.hridaya.kubenexus.data.source.local.entity.NamespaceEntity
import dev.hridaya.kubenexus.data.source.local.entity.PodEntity
import dev.hridaya.kubenexus.domain.model.ContainerDetail
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodConditionDetail
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.PodEventDetail
import dev.hridaya.kubenexus.domain.model.PodStatus
import org.json.JSONObject

fun NativePod.toDomain(): Pod {
    val domainStatus = when (status?.lowercase()) {
        "running" -> PodStatus.RUNNING
        "pending" -> PodStatus.PENDING
        "completed", "succeeded" -> PodStatus.COMPLETED
        "failed" -> PodStatus.FAILED
        "crashloopbackoff", "error" -> PodStatus.CRASH_LOOP
        else -> PodStatus.UNKNOWN
    }

    return Pod(
        id = "${namespace ?: "default"}_${name ?: "pod"}",
        name = name ?: "unknown",
        namespace = namespace ?: "default",
        status = domainStatus,
        readyContainers = ready?.ifBlank { "1/1" } ?: "1/1",
        restarts = restarts,
        age = age?.ifBlank { "0m" } ?: "0m",
        node = node?.ifBlank { null },
        ip = ip?.ifBlank { null },
    )
}

fun Pod.toNative(): NativePod {
    val nativePod = NativePod()
    nativePod.name = name
    nativePod.namespace = namespace
    nativePod.status = status.title
    nativePod.ready = readyContainers
    nativePod.restarts = restarts
    nativePod.age = age
    node?.let { nativePod.node = it }
    ip?.let { nativePod.ip = it }
    return nativePod
}

fun NativePodDetails.toDomain(): PodDetails {
    val domainStatus = when (status?.lowercase()) {
        "running" -> PodStatus.RUNNING
        "pending" -> PodStatus.PENDING
        "completed", "succeeded" -> PodStatus.COMPLETED
        "failed" -> PodStatus.FAILED
        "crashloopbackoff", "error" -> PodStatus.CRASH_LOOP
        else -> PodStatus.UNKNOWN
    }

    val containerList = mutableListOf<ContainerDetail>()
    val nativeContainers = containers()
    if (nativeContainers != null) {
        val len = nativeContainers.len()
        for (i in 0 until len) {
            val c = nativeContainers.get(i)
            if (c != null) {
                containerList.add(c.toDomain())
            }
        }
    }

    val initContainerList = mutableListOf<ContainerDetail>()
    val nativeInitContainers = initContainers()
    if (nativeInitContainers != null) {
        val len = nativeInitContainers.len()
        for (i in 0 until len) {
            val c = nativeInitContainers.get(i)
            if (c != null) {
                initContainerList.add(c.toDomain())
            }
        }
    }

    val conditionList = mutableListOf<PodConditionDetail>()
    val nativeConditions = conditions()
    if (nativeConditions != null) {
        val len = nativeConditions.len()
        for (i in 0 until len) {
            val cond = nativeConditions.get(i)
            if (cond != null) {
                conditionList.add(cond.toDomain())
            }
        }
    }

    val eventList = mutableListOf<PodEventDetail>()
    val nativeEvents = events()
    if (nativeEvents != null) {
        val len = nativeEvents.len()
        for (i in 0 until len) {
            val ev = nativeEvents.get(i)
            if (ev != null) {
                eventList.add(ev.toDomain())
            }
        }
    }

    val volumesList = mutableListOf<String>()
    val nativeVolumes = volumes()
    if (nativeVolumes != null) {
        val len = nativeVolumes.len()
        for (i in 0 until len) {
            val v = nativeVolumes.get(i)
            if (!v.isNullOrBlank()) {
                volumesList.add(v)
            }
        }
    }
    if (volumesList.isEmpty() && !volumesCSV.isNullOrBlank()) {
        volumesList.addAll(volumesCSV.split(",").map { it.trim() }.filter { it.isNotBlank() })
    }

    val labelsMap = mutableMapOf<String, String>()
    if (!labelsJSON.isNullOrBlank()) {
        try {
            val json = JSONObject(labelsJSON)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                labelsMap[k] = json.optString(k)
            }
        } catch (_: Exception) {
        }
    }

    return PodDetails(
        name = name ?: "",
        namespace = namespace ?: "default",
        status = domainStatus,
        node = node,
        ip = ip,
        hostIp = hostIP,
        restartPolicy = restartPolicy ?: "Always",
        startTime = startTime,
        containers = containerList,
        initContainers = initContainerList,
        conditions = conditionList,
        events = eventList,
        labels = labelsMap,
        annotations = emptyMap(),
        volumes = volumesList,
        rawDescribeText = json(),
    )
}

fun NativeContainerInfo.toDomain(): ContainerDetail {
    return ContainerDetail(
        name = name ?: "",
        image = image ?: "",
        ready = ready,
        restartCount = restartCount,
        state = state ?: "Running",
    )
}

fun NativePodCondition.toDomain(): PodConditionDetail {
    return PodConditionDetail(
        type = type ?: "",
        status = status ?: "",
        lastTransitionTime = lastTransitionTime,
        reason = reason,
        message = message,
    )
}

fun NativePodEvent.toDomain(): PodEventDetail {
    return PodEventDetail(
        type = type ?: "Normal",
        reason = reason ?: "",
        message = message ?: "",
        age = age ?: "",
    )
}

fun PodEntity.toDomain(): Pod {
    val domainStatus = when (status.lowercase()) {
        "running" -> PodStatus.RUNNING
        "pending" -> PodStatus.PENDING
        "completed", "succeeded" -> PodStatus.COMPLETED
        "failed" -> PodStatus.FAILED
        "crashloopbackoff", "error" -> PodStatus.CRASH_LOOP
        else -> PodStatus.UNKNOWN
    }

    return Pod(
        id = id,
        name = name,
        namespace = namespace,
        status = domainStatus,
        readyContainers = readyContainers,
        restarts = restarts,
        age = age,
        node = node,
        ip = ip,
        image = image,
    )
}

fun Pod.toEntity(clusterId: String): PodEntity {
    return PodEntity(
        id = "${clusterId}_${namespace}_$name",
        clusterId = clusterId,
        name = name,
        namespace = namespace,
        status = status.title,
        readyContainers = readyContainers,
        restarts = restarts,
        age = age,
        ip = ip,
        node = node,
        image = image,
    )
}

fun NamespaceEntity.toDomainName(): String = name

fun NativeNamespace.toDomainName(): String = name ?: "default"
