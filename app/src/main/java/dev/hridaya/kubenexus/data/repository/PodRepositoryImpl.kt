package dev.hridaya.kubenexus.data.repository

import android.util.Log
import client.LogCallback
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.core.security.KubeconfigEncryptor
import dev.hridaya.kubenexus.core.security.LogSanitizer
import dev.hridaya.kubenexus.core.security.NoOpKubeconfigEncryptor
import dev.hridaya.kubenexus.data.mapper.toDomain
import dev.hridaya.kubenexus.data.mapper.toDomainName
import dev.hridaya.kubenexus.data.mapper.toEntity
import dev.hridaya.kubenexus.data.source.local.dao.ClusterDao
import dev.hridaya.kubenexus.data.source.local.dao.NamespaceDao
import dev.hridaya.kubenexus.data.source.local.dao.PodDao
import dev.hridaya.kubenexus.data.source.local.entity.NamespaceEntity
import dev.hridaya.kubenexus.data.source.remote.KubernetesApiClient
import dev.hridaya.kubenexus.domain.model.CommandExecResult
import dev.hridaya.kubenexus.domain.model.Pod
import dev.hridaya.kubenexus.domain.model.PodDetails
import dev.hridaya.kubenexus.domain.model.TerminalSession
import dev.hridaya.kubenexus.domain.repository.PodRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NativeTerminalSession(
    private val session: client.ExecSession
) : TerminalSession {
    override fun write(input: String) {
        session.write(input)
    }

    override fun writeBytes(bytes: ByteArray) {
        session.writeBytes(bytes)
    }

    override fun close() {
        session.close()
    }
}

class PodRepositoryImpl(
    private val clusterDao: ClusterDao,
    private val podDao: PodDao,
    private val namespaceDao: NamespaceDao,
    private val apiClient: KubernetesApiClient,
    private val nativeBridge: KubeNexusNativeBridge,
    private val encryptor: KubeconfigEncryptor = NoOpKubeconfigEncryptor,
    private val dispatcherProvider: DispatcherProvider
) : PodRepository {

    companion object {
        private const val TAG = "PodRepositoryImpl"
    }

    override fun getPodsStream(clusterId: String?, namespace: String?): Flow<List<Pod>> {
        if (clusterId == null) return flowOf(emptyList())

        val stream = if (namespace.isNullOrBlank() || namespace == "All Namespaces" || namespace.equals("all", ignoreCase = true)) {
            podDao.getPodsStream(clusterId)
        } else {
            podDao.getPodsByNamespaceStream(clusterId, namespace.trim())
        }

        return stream.map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(dispatcherProvider.io)
    }

    override fun getNamespacesStream(clusterId: String?): Flow<List<String>> {
        if (clusterId == null) return flowOf(listOf("All Namespaces"))

        return namespaceDao.getNamespacesStream(clusterId).map { entities ->
            val nsList = entities.map { it.toDomainName() }.filter { it.isNotBlank() }.distinct().sorted()
            if (nsList.isNotEmpty()) {
                listOf("All Namespaces") + nsList
            } else {
                listOf("All Namespaces", "default", "kube-system")
            }
        }.flowOn(dispatcherProvider.io)
    }

    override fun getLastRefreshedStream(clusterId: String?): Flow<Long?> {
        if (clusterId == null) return flowOf(null)
        return podDao.getSyncMetadataStream("${clusterId}_pods").flowOn(dispatcherProvider.io)
    }

    override suspend fun refreshWorkloads(clusterId: String?, namespace: String?): Result<Unit> {
        return withContext(dispatcherProvider.io) {
            if (clusterId == null) {
                return@withContext Result.Error(AppError.NotFound("No active cluster specified"))
            }

            val cluster = clusterDao.getClusterById(clusterId)
                ?: return@withContext Result.Error(AppError.NotFound("Cluster with ID '$clusterId' not found"))

            val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)
            val queryNamespace = if (namespace.isNullOrBlank() || namespace == "All Namespaces" || namespace.equals("all", ignoreCase = true)) null else namespace.trim()

            try {
                val livePods: List<Pod> = try {
                    val nativeResult = nativeBridge.listPodsWide(decryptedKubeconfig, queryNamespace).getOrNull()
                    if (nativeResult != null) {
                        nativeResult.map { it.toDomain() }
                    } else {
                        apiClient.fetchPods(
                            serverUrl = cluster.serverUrl,
                            rawKubeconfig = decryptedKubeconfig,
                            namespace = queryNamespace
                        )
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Native listPodsWide fallback to HTTP client: ${LogSanitizer.sanitize(t.message)}")
                    apiClient.fetchPods(
                        serverUrl = cluster.serverUrl,
                        rawKubeconfig = decryptedKubeconfig,
                        namespace = queryNamespace
                    )
                }

                val liveNamespaces: List<String> = try {
                    val nativeNsResult = nativeBridge.listNamespaces(decryptedKubeconfig).getOrNull()
                    if (!nativeNsResult.isNullOrEmpty()) {
                        nativeNsResult.map { it.toDomainName() }
                    } else {
                        apiClient.fetchNamespaces(
                            serverUrl = cluster.serverUrl,
                            rawKubeconfig = decryptedKubeconfig
                        )
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Native listNamespaces fallback: ${LogSanitizer.sanitize(t.message)}")
                    apiClient.fetchNamespaces(
                        serverUrl = cluster.serverUrl,
                        rawKubeconfig = decryptedKubeconfig
                    )
                }

                val podEntities = livePods.map { it.toEntity(clusterId) }
                podDao.syncPods(
                    clusterId = clusterId,
                    namespace = queryNamespace,
                    pods = podEntities,
                    timestamp = System.currentTimeMillis()
                )

                if (liveNamespaces.isNotEmpty()) {
                    val namespaceEntities = liveNamespaces
                        .filter { it != "All Namespaces" }
                        .map { name ->
                            NamespaceEntity(
                                id = "${clusterId}_$name",
                                clusterId = clusterId,
                                name = name
                            )
                        }
                    namespaceDao.syncNamespaces(clusterId, namespaceEntities)
                }

                Result.Success(Unit)
            } catch (t: Throwable) {
                val sanitizedMsg = LogSanitizer.sanitize(t.message)
                Log.e(TAG, "Failed to refresh workloads for cluster '$clusterId': $sanitizedMsg", t)
                Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to connect to cluster API" }))
            }
        }
    }

    override suspend fun describePod(
        clusterId: String?,
        namespace: String,
        podName: String
    ): Result<PodDetails> = withContext(dispatcherProvider.io) {
        if (clusterId == null) return@withContext Result.Error(AppError.NotFound("No cluster selected"))
        val cluster = clusterDao.getClusterById(clusterId)
            ?: return@withContext Result.Error(AppError.NotFound("Cluster '$clusterId' not found"))

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)

        try {
            val nativeResult = nativeBridge.describePod(decryptedKubeconfig, namespace, podName)
            if (nativeResult.isSuccess) {
                val nativePodDetails = nativeResult.getOrThrow()
                return@withContext Result.Success(nativePodDetails.toDomain())
            }

            val details = apiClient.describePod(
                serverUrl = cluster.serverUrl,
                rawKubeconfig = decryptedKubeconfig,
                namespace = namespace,
                podName = podName
            )
            Result.Success(details)
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Failed to describe pod '$podName': $sanitizedMsg", t)
            Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to describe pod from cluster API" }))
        }
    }

    override suspend fun deletePod(
        clusterId: String?,
        namespace: String,
        podName: String
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        if (clusterId == null) return@withContext Result.Error(AppError.NotFound("No cluster selected"))
        val cluster = clusterDao.getClusterById(clusterId)
            ?: return@withContext Result.Error(AppError.NotFound("Cluster '$clusterId' not found"))

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)

        try {
            val nativeResult = nativeBridge.deletePod(decryptedKubeconfig, namespace, podName)
            if (nativeResult.isSuccess) {
                val podId = "${clusterId}_${namespace}_$podName"
                podDao.deletePod(podId)
                Result.Success(Unit)
            } else {
                val error = nativeResult.exceptionOrNull()
                val sanitizedMsg = LogSanitizer.sanitize(error?.message)
                Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to delete pod $podName" }))
            }
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Failed to delete pod '$podName': $sanitizedMsg", t)
            Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to delete pod" }))
        }
    }

    override suspend fun getPodLogs(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String?
    ): Result<String> = withContext(dispatcherProvider.io) {
        if (clusterId == null) return@withContext Result.Error(AppError.NotFound("No cluster selected"))
        val cluster = clusterDao.getClusterById(clusterId)
            ?: return@withContext Result.Error(AppError.NotFound("Cluster '$clusterId' not found"))

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)

        try {
            val nativeResult = nativeBridge.getPodLogs(decryptedKubeconfig, namespace, podName, containerName)
            if (nativeResult.isSuccess) {
                return@withContext Result.Success(nativeResult.getOrThrow())
            }

            val logs = apiClient.fetchPodLogs(
                serverUrl = cluster.serverUrl,
                rawKubeconfig = decryptedKubeconfig,
                namespace = namespace,
                podName = podName,
                containerName = containerName
            )
            Result.Success(logs)
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Failed to fetch logs for pod '$podName': $sanitizedMsg", t)
            Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to fetch logs from cluster API" }))
        }
    }

    override fun streamPodLogs(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String?
    ): Flow<String> = callbackFlow {
        if (clusterId == null) {
            trySend("Error: No active cluster selected")
            close()
            return@callbackFlow
        }
        val cluster = clusterDao.getClusterById(clusterId)
        if (cluster == null) {
            trySend("Error: Cluster '$clusterId' not found in database")
            close()
            return@callbackFlow
        }

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)

        var isStreamClosed = false
        val logCallback = object : LogCallback {
            override fun onLogLine(line: String) {
                if (!isStreamClosed) {
                    trySend(line)
                }
            }

            override fun onError(err: String) {
                if (!isStreamClosed) {
                    trySend("[Log error] ${LogSanitizer.sanitize(err)}")
                }
            }

            override fun onDone() {
                if (!isStreamClosed) {
                    isStreamClosed = true
                    close()
                }
            }
        }

        val nativeResult = nativeBridge.streamPodLogs(
            rawKubeconfig = decryptedKubeconfig,
            namespace = namespace,
            podName = podName,
            container = containerName,
            callback = logCallback
        )

        if (nativeResult.isFailure) {
            Log.w(TAG, "Native streamLogs fallback to HTTP: ${LogSanitizer.sanitize(nativeResult.exceptionOrNull()?.message)}")
            val job = launch(dispatcherProvider.io) {
                apiClient.streamPodLogs(
                    serverUrl = cluster.serverUrl,
                    rawKubeconfig = decryptedKubeconfig,
                    namespace = namespace,
                    podName = podName,
                    containerName = containerName
                ).collect { line ->
                    trySend(line)
                }
            }
            awaitClose { job.cancel() }
        } else {
            awaitClose {
                isStreamClosed = true
            }
        }
    }.flowOn(dispatcherProvider.io)

    override suspend fun execCommand(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        command: String,
        stdin: String
    ): Result<CommandExecResult> = withContext(dispatcherProvider.io) {
        if (clusterId == null) return@withContext Result.Error(AppError.NotFound("No cluster selected"))
        val cluster = clusterDao.getClusterById(clusterId)
            ?: return@withContext Result.Error(AppError.NotFound("Cluster '$clusterId' not found"))

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)

        try {
            val result = nativeBridge.exec(
                rawKubeconfig = decryptedKubeconfig,
                namespace = namespace,
                podName = podName,
                container = containerName,
                command = command,
                stdin = stdin
            )
            if (result.isSuccess) {
                val nativeRes = result.getOrThrow()
                Result.Success(
                    CommandExecResult(
                        stdout = nativeRes.stdout.orEmpty(),
                        stderr = nativeRes.stderr.orEmpty()
                    )
                )
            } else {
                val ex = result.exceptionOrNull()
                val sanitizedMsg = LogSanitizer.sanitize(ex?.message)
                Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to exec command" }))
            }
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Exec command error on pod '$podName': $sanitizedMsg", t)
            Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to exec command" }))
        }
    }

    override suspend fun startTerminalSession(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ): Result<TerminalSession> = withContext(dispatcherProvider.io) {
        if (clusterId == null) return@withContext Result.Error(AppError.NotFound("No cluster selected"))
        val cluster = clusterDao.getClusterById(clusterId)
            ?: return@withContext Result.Error(AppError.NotFound("Cluster '$clusterId' not found"))

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)

        try {
            val callback = object : client.ExecCallback {
                override fun onStdout(output: String) {
                    onStdout(output)
                }

                override fun onStderr(output: String) {
                    onStderr(output)
                }

                override fun onError(err: String) {
                    onError(LogSanitizer.sanitize(err))
                }

                override fun onDone() {
                    onDone()
                }
            }

            val result = nativeBridge.startTerminal(
                rawKubeconfig = decryptedKubeconfig,
                namespace = namespace,
                podName = podName,
                container = containerName,
                callback = callback
            )
            if (result.isSuccess) {
                Result.Success(NativeTerminalSession(result.getOrThrow()))
            } else {
                val ex = result.exceptionOrNull()
                val sanitizedMsg = LogSanitizer.sanitize(ex?.message)
                Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to start terminal session" }))
            }
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Start terminal error on pod '$podName': $sanitizedMsg", t)
            Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to start terminal session" }))
        }
    }

    override suspend fun startExecSession(
        clusterId: String?,
        namespace: String,
        podName: String,
        containerName: String,
        command: String,
        tty: Boolean,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ): Result<TerminalSession> = withContext(dispatcherProvider.io) {
        if (clusterId == null) return@withContext Result.Error(AppError.NotFound("No cluster selected"))
        val cluster = clusterDao.getClusterById(clusterId)
            ?: return@withContext Result.Error(AppError.NotFound("Cluster '$clusterId' not found"))

        val decryptedKubeconfig = encryptor.decrypt(cluster.rawKubeconfig)

        try {
            val callback = object : client.ExecCallback {
                override fun onStdout(output: String) {
                    onStdout(output)
                }

                override fun onStderr(output: String) {
                    onStderr(output)
                }

                override fun onError(err: String) {
                    onError(LogSanitizer.sanitize(err))
                }

                override fun onDone() {
                    onDone()
                }
            }

            val result = nativeBridge.startExecSession(
                rawKubeconfig = decryptedKubeconfig,
                namespace = namespace,
                podName = podName,
                container = containerName,
                command = command,
                tty = tty,
                callback = callback
            )
            if (result.isSuccess) {
                Result.Success(NativeTerminalSession(result.getOrThrow()))
            } else {
                val ex = result.exceptionOrNull()
                val sanitizedMsg = LogSanitizer.sanitize(ex?.message)
                Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to start exec session" }))
            }
        } catch (t: Throwable) {
            val sanitizedMsg = LogSanitizer.sanitize(t.message)
            Log.e(TAG, "Start exec session error on pod '$podName': $sanitizedMsg", t)
            Result.Error(AppError.Network(sanitizedMsg.ifEmpty { "Failed to start exec session" }))
        }
    }
}
