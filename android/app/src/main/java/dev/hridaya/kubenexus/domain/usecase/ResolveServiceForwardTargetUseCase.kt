package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.domain.model.PodStatus
import dev.hridaya.kubenexus.domain.model.ServiceDetails
import dev.hridaya.kubenexus.domain.model.ServiceForwardTarget
import javax.inject.Inject

/**
 * Resolves a concrete pod name and numeric container port destination for
 * forwarding into a Service.
 *
 * Kubernetes Services do not listen directly; kubectl port-forward service/name
 * queries the matching pods, selects an active endpoint pod, and resolves
 * targetPort (which may be numeric or a named container port).
 */
class ResolveServiceForwardTargetUseCase @Inject constructor(
    private val nativeBridge: KubeNexusNativeBridge,
) {
    suspend operator fun invoke(
        rawKubeconfig: String,
        service: ServiceDetails,
        servicePort: Int,
    ): Result<ServiceForwardTarget> {
        if (service.selector.isEmpty()) {
            return Result.Error(
                AppError.Validation("Service '${service.name}' has no selector defined to target pods."),
            )
        }

        val labelSelector = service.selector.entries.joinToString(",") { "${it.key}=${it.value}" }
        val podsResult = nativeBridge.listPods(
            rawKubeconfig = rawKubeconfig,
            namespace = service.namespace,
            labelSelector = labelSelector,
        )

        val pods = when (podsResult) {
            is Result.Success -> podsResult.data
            is Result.Error -> return Result.Error(podsResult.error)
            is Result.Loading -> return Result.Error(AppError.Validation("Pods loading"))
        }

        val targetPod = pods.firstOrNull { pod ->
            pod.status == PodStatus.RUNNING && pod.readyContainers.split("/").let { parts ->
                parts.size == 2 && parts[0] == parts[1] && parts[0] != "0"
            }
        } ?: pods.firstOrNull { it.status == PodStatus.RUNNING }
            ?: pods.firstOrNull()
            ?: return Result.Error(
                AppError.NotFound("No active pods found matching selector for service '${service.name}'."),
            )

        val portDetail = service.ports.firstOrNull { it.port == servicePort }
        val targetPort = if (portDetail != null && portDetail.targetPort > 0) {
            portDetail.targetPort
        } else {
            servicePort
        }

        return Result.Success(
            ServiceForwardTarget(
                podName = targetPod.name,
                podPort = targetPort,
            ),
        )
    }
}
