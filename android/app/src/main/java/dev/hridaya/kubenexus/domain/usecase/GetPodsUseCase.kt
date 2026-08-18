package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.repository.PodRepository
import kotlinx.coroutines.flow.Flow

class GetPodsUseCase(private val podRepository: PodRepository) {
    operator fun invoke(clusterId: String?, namespace: String? = null): Flow<List<Pod>> {
        return podRepository.getPodsStream(clusterId, namespace)
    }
}
