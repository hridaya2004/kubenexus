package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class GetActiveClusterUseCase(private val repository: ClusterRepository, private val dispatcherProvider: DispatcherProvider) {
    operator fun invoke(): Flow<Cluster?> {
        return repository.getActiveClusterStream().flowOn(dispatcherProvider.io)
    }
}
