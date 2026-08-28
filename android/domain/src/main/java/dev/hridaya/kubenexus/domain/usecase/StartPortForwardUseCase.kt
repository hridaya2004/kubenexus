package dev.hridaya.kubenexus.domain.usecase

import dev.hridaya.kubenexus.core.common.result.Result
import dev.hridaya.kubenexus.domain.model.PortForwardListener
import dev.hridaya.kubenexus.domain.repository.PortForwardRepository
import javax.inject.Inject

/**
 * Opens a port-forward tunnel from 127.0.0.1:[localPort] to [remotePort] on the
 * given pod. Returns the opaque handle id used to stop the session later; the
 * tunnel is usable once [PortForwardListener.onPortForwardReady] fires.
 */
class StartPortForwardUseCase @Inject constructor(private val repository: PortForwardRepository) {
    suspend operator fun invoke(
        rawKubeconfig: String,
        namespace: String,
        podName: String,
        localPort: Int,
        remotePort: Int,
        listener: PortForwardListener,
    ): Result<String> {
        return repository.start(rawKubeconfig, namespace, podName, localPort, remotePort, listener)
    }
}
