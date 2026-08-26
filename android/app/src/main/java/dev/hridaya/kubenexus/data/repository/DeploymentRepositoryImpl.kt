package dev.hridaya.kubenexus.data.repository

import android.util.Log
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.core.security.KubeconfigEncryptor
import dev.hridaya.kubenexus.core.security.LogSanitizer
import dev.hridaya.kubenexus.core.security.NoOpKubeconfigEncryptor
import dev.hridaya.kubenexus.data.mapper.toDomain
import dev.hridaya.kubenexus.data.mapper.toEntity
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.dao.DeploymentDao
import dev.hridaya.kubenexus.domain.model.DeploymentDetails
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeploymentRepositoryImpl @Inject constructor(
    private val clusterDao: ClusterDao,
    private val deploymentDao: DeploymentDao,
    private val nativeBridge: KubeNexusNativeBridge,
    private val encryptor: KubeconfigEncryptor = NoOpKubeconfigEncryptor,
    private val dispatcherProvider: DispatcherProvider,
) : DeploymentRepository {

    companion object {
        private const val TAG = "DeploymentRepositoryImpl"
    }

    override suspend fun createFromManifest(
        clusterId: String?,
        manifestYaml: String,
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        if (clusterId == null) return@withContext Result.Error(AppError.NotFound("No cluster selected"))
        val cluster = clusterDao.getClusterById(clusterId)
            ?: return@withContext Result.Error(AppError.NotFound("Cluster '$clusterId' not found"))

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)

        try {
            // The reviewed manifest carries its own namespace, so the bridge is
            // told to fall back to it rather than overriding the user's choice.
            val nativeResult = nativeBridge.createDeployment(decryptedKubeconfig, "", manifestYaml)
            if (nativeResult.isSuccess) {
                Result.Success(Unit)
            } else {
                val error = nativeResult.exceptionOrNull()
                val sanitizedMsg = LogSanitizer.sanitize(error?.message)
                Log.e(TAG, "Failed to create deployment for cluster '$clusterId': $sanitizedMsg")
                Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to create deployment" }))
            }
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Failed to create deployment for cluster '$clusterId': $sanitizedMsg", t)
            Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to create deployment" }))
        }
    }

    override suspend fun getDeployments(
        clusterId: String?,
        namespace: String?,
    ): Result<List<DeploymentSummary>> = withContext(dispatcherProvider.io) {
        if (clusterId == null) return@withContext Result.Error(AppError.NotFound("No cluster selected"))
        val cluster = clusterDao.getClusterById(clusterId)
            ?: return@withContext Result.Error(AppError.NotFound("Cluster '$clusterId' not found"))

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)

        try {
            val nativeResult = nativeBridge.listDeployments(decryptedKubeconfig, namespace)
            if (nativeResult.isSuccess) {
                Result.Success(nativeResult.getOrThrow())
            } else {
                val ex = nativeResult.exceptionOrNull()
                val sanitizedMsg = LogSanitizer.sanitize(ex?.message)
                Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to load deployments" }))
            }
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Failed to load deployments for cluster '$clusterId': $sanitizedMsg", t)
            Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to load deployments" }))
        }
    }

    override fun getDeploymentsStream(
        clusterId: String?,
        namespace: String?,
    ): Flow<List<DeploymentSummary>> {
        if (clusterId == null) return flowOf(emptyList())

        val stream =
            if (isAllNamespaces(namespace)) {
                deploymentDao.getDeploymentsStream(clusterId)
            } else {
                deploymentDao.getDeploymentsByNamespaceStream(clusterId, namespace!!.trim())
            }

        return stream.map { entities ->
            entities.map { entity -> entity.toDomain() }
        }.flowOn(dispatcherProvider.io)
    }

    override suspend fun syncDeployments(
        clusterId: String?,
        namespace: String?,
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        if (clusterId == null) {
            return@withContext Result.Error(AppError.NotFound("No active cluster specified"))
        }

        val cluster = clusterDao.getClusterById(clusterId)
            ?: return@withContext Result.Error(AppError.NotFound("Cluster with ID '$clusterId' not found"))

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)
        val queryNamespace = normalizedNamespaceOrNull(namespace)

        try {
            val nativeResult = nativeBridge.listDeployments(decryptedKubeconfig, queryNamespace)
            if (nativeResult.isFailure) {
                val ex = nativeResult.exceptionOrNull()
                val sanitizedMsg = LogSanitizer.sanitize(ex?.message)
                return@withContext Result.Error(
                    AppError.Network(sanitizedMsg.ifEmpty { "Failed to list deployments from cluster" }),
                )
            }
            val liveDeployments: List<DeploymentSummary> = nativeResult.getOrThrow()

            deploymentDao.syncDeployments(
                clusterId = clusterId,
                namespace = queryNamespace,
                deployments = liveDeployments.map { it.toEntity(clusterId) },
                timestamp = System.currentTimeMillis(),
            )

            Result.Success(Unit)
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Failed to sync deployments for cluster '$clusterId': $sanitizedMsg", t)
            Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to connect to cluster API" }))
        }
    }

    override suspend fun getDeploymentDetails(
        clusterId: String?,
        namespace: String,
        name: String,
    ): Result<DeploymentDetails> = withContext(dispatcherProvider.io) {
        if (clusterId == null) return@withContext Result.Error(AppError.NotFound("No cluster selected"))
        val cluster = clusterDao.getClusterById(clusterId)
            ?: return@withContext Result.Error(AppError.NotFound("Cluster '$clusterId' not found"))

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)

        try {
            // The bridge assembles details, including best-effort events.
            val nativeResult = nativeBridge.describeDeployment(decryptedKubeconfig, namespace, name)
            if (nativeResult.isSuccess) {
                Result.Success(nativeResult.getOrThrow())
            } else {
                val ex = nativeResult.exceptionOrNull()
                val sanitizedMsg = LogSanitizer.sanitize(ex?.message)
                Result.Error(
                    AppError.Network(sanitizedMsg.ifEmpty { "Failed to describe deployment '$name'" }),
                )
            }
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Failed to describe deployment '$name': $sanitizedMsg", t)
            Result.Error(
                AppError.Network(sanitizedMsg.ifEmpty { "Failed to describe deployment from cluster API" }),
            )
        }
    }

    /** Mirrors how the pods screen expresses "no namespace filter". */
    private fun isAllNamespaces(namespace: String?): Boolean =
        namespace.isNullOrBlank() ||
            namespace == "All Namespaces" ||
            namespace.equals("all", ignoreCase = true)

    /**
     * The bridge normalizes its own namespace argument; the DAO needs the null
     * form so its sync scope matches what was actually fetched.
     */
    private fun normalizedNamespaceOrNull(namespace: String?): String? =
        if (isAllNamespaces(namespace)) null else namespace?.trim()
}
