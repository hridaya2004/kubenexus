package dev.hridaya.kubenexus.domain.repository

import dev.hridaya.kubenexus.domain.model.Pod
import kotlinx.coroutines.flow.Flow

interface PodRepository {
    fun getPodsStream(clusterId: String?, namespace: String? = null): Flow<List<Pod>>
    fun getNamespacesStream(clusterId: String?): Flow<List<String>>
}
