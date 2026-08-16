package dev.hridaya.kubenexus.data.mapper

import client.Namespace as NativeNamespace
import client.Pod as NativePod
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodStatus

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
        ip = ip?.ifBlank { null }
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

fun NativeNamespace.toDomainName(): String = name ?: "default"
