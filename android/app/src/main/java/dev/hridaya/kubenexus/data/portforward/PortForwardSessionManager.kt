package dev.hridaya.kubenexus.data.portforward

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.hridaya.kubenexus.core.common.dispatcher.DispatcherProvider
import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.core.di.ApplicationScope
import dev.hridaya.kubenexus.core.nativebridge.PortForwardListener
import dev.hridaya.kubenexus.domain.repository.PortForwardRepository
import dev.hridaya.kubenexus.presentation.portforward.sessions.ActivePortForwardSession
import dev.hridaya.kubenexus.presentation.portforward.sessions.PortForwardSessionStatus
import dev.hridaya.kubenexus.presentation.portforward.sessions.PortForwardTargetKind
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Process-wide manager and single source of truth for all active port-forward sessions.
 *
 * Coordinates between the native bridge / repository and presentation layer,
 * keeping track of Pod and Service forward lifecycles and notifying subscribers
 * of state transitions (starting, ready, error, stopped).
 */
@Singleton
class PortForwardSessionManager @Inject constructor(
    private val repository: PortForwardRepository,
    @param:ApplicationScope private val externalScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    @param:ApplicationContext private val context: Context? = null,
    private val notificationManager: PortForwardNotificationManager? = null,
) {
    private val _sessions = MutableStateFlow<List<ActivePortForwardSession>>(emptyList())
    val sessions: StateFlow<List<ActivePortForwardSession>> = _sessions.asStateFlow()

    init {
        externalScope.launch(dispatcherProvider.io) {
            _sessions.collect { list ->
                val active = list.filter { it.isActive }
                if (active.isNotEmpty()) {
                    context?.let { ctx ->
                        try {
                            ContextCompat.startForegroundService(
                                ctx,
                                Intent(ctx, PortForwardService::class.java),
                            )
                        } catch (_: Exception) {
                            // Foreground service launch exception fallback
                        }
                    }
                } else {
                    notificationManager?.dismissNotification()
                }
            }
        }
    }

    private val internalListener = object : PortForwardListener {
        override fun onPortForwardReady(handleId: String, localPort: Int) {
            updateSession(handleId) { it.copy(status = PortForwardSessionStatus.READY) }
        }

        override fun onPortForwardError(handleId: String, message: String) {
            updateSession(handleId) {
                it.copy(
                    status = PortForwardSessionStatus.ERROR,
                    message = message,
                )
            }
        }

        override fun onPortForwardStopped(handleId: String, reason: String) {
            updateSession(handleId) { it.copy(status = PortForwardSessionStatus.STOPPED) }
        }
    }

    suspend fun startPodForward(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        localPort: Int,
        remotePort: Int,
    ): Result<String> {
        val result = repository.start(
            rawKubeconfig = rawKubeconfig,
            namespace = namespace,
            podName = podName,
            localPort = localPort,
            remotePort = remotePort,
            listener = internalListener,
        )
        if (result is Result.Success) {
            val session = ActivePortForwardSession(
                handleId = result.data,
                kind = PortForwardTargetKind.Pod,
                namespace = namespace,
                targetName = podName,
                podName = podName,
                localPort = localPort,
                remotePort = remotePort,
                status = PortForwardSessionStatus.STARTING,
            )
            addSession(session)
        }
        return result
    }

    suspend fun startServiceForward(
        rawKubeconfig: String,
        namespace: String,
        serviceName: String,
        localPort: Int,
        servicePort: Int,
        targetPodName: String,
        targetPodPort: Int,
    ): Result<String> {
        val result = repository.start(
            rawKubeconfig = rawKubeconfig,
            namespace = namespace,
            podName = targetPodName,
            localPort = localPort,
            remotePort = targetPodPort,
            listener = internalListener,
        )
        if (result is Result.Success) {
            val session = ActivePortForwardSession(
                handleId = result.data,
                kind = PortForwardTargetKind.Service,
                namespace = namespace,
                targetName = serviceName,
                podName = targetPodName,
                localPort = localPort,
                remotePort = servicePort,
                status = PortForwardSessionStatus.STARTING,
            )
            addSession(session)
        }
        return result
    }

    suspend fun stop(handleId: String): Result<Unit> {
        val result = repository.stop(handleId)
        updateSession(handleId) { it.copy(status = PortForwardSessionStatus.STOPPED) }
        return result
    }

    fun stopAll() {
        val activeHandles = _sessions.value.filter { it.isActive }.map { it.handleId }
        externalScope.launch(dispatcherProvider.io) {
            activeHandles.forEach { handleId ->
                launch {
                    repository.stop(handleId)
                    updateSession(handleId) { it.copy(status = PortForwardSessionStatus.STOPPED) }
                }
            }
        }
    }

    private fun addSession(session: ActivePortForwardSession) {
        _sessions.update { list ->
            val index = list.indexOfFirst { it.handleId == session.handleId }
                .takeIf { it >= 0 }
                ?: list.indexOfFirst { it.localPort == session.localPort && it.isActive }
            if (index < 0) {
                list + session
            } else {
                list.toMutableList().apply { this[index] = session }
            }
        }
    }

    private fun updateSession(
        handleId: String,
        transform: (ActivePortForwardSession) -> ActivePortForwardSession,
    ) {
        _sessions.update { list ->
            val index = list.indexOfFirst { it.handleId == handleId }
            if (index < 0) {
                list
            } else {
                list.toMutableList().apply { this[index] = transform(this[index]) }
            }
        }
    }
}
