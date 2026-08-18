package dev.hridaya.kubenexus

import android.app.Application
import dev.hridaya.kubenexus.core.di.AppContainer
import dev.hridaya.kubenexus.core.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KubeNexusApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
        container.nativeBridge.initialize()

        // Migrate any legacy plaintext cluster kubeconfigs to Keystore-backed encryption
        applicationScope.launch {
            container.clusterRepository.migratePlaintextClusters()
        }
    }
}
