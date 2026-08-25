package dev.hridaya.kubenexus.data.repository

import android.util.Log
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.core.security.KubeconfigEncryptor
import dev.hridaya.kubenexus.core.security.LogSanitizer
import dev.hridaya.kubenexus.core.security.NoOpKubeconfigEncryptor
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.domain.model.DeploymentSummary
import dev.hridaya.kubenexus.domain.repository.DeploymentRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeploymentRepositoryImpl @Inject constructor(
    private val clusterDao: ClusterDao,
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
}
