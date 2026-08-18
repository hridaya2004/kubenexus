package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.Cluster
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import kotlinx.coroutines.withContext

class AddClusterUseCase(
    private val repository: ClusterRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(
        kubeconfigRaw: String,
        customName: String? = null,
        setAsActive: Boolean = true
    ): Result<Cluster> = withContext(dispatcherProvider.io) {
        repository.addCluster(
            kubeconfigRaw = kubeconfigRaw,
            customName = customName,
            setAsActive = setAsActive,
        )
    }
}
