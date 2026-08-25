package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.PodMetricSample
import dev.hridaya.kubenexus.domain.repository.PodRepository
import javax.inject.Inject

class GetPodMetricsUseCase @Inject constructor(private val podRepository: PodRepository) {
    suspend operator fun invoke(
        clusterId: String?,
        namespace: String?,
    ): Result<List<PodMetricSample>> {
        return podRepository.getPodMetrics(clusterId, namespace)
    }

    /**
     * Usage for one pod. Used by the detail screen, which polls continuously and
     * would otherwise refetch the entire namespace on every tick.
     */
    suspend fun forPod(
        clusterId: String?,
        namespace: String,
        podName: String,
    ): Result<PodMetricSample?> {
        return podRepository.getSinglePodMetrics(clusterId, namespace, podName)
    }
}
