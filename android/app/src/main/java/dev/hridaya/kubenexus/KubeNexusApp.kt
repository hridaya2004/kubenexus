package dev.hridaya.kubenexus

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.hridaya.kubenexus.core.di.ApplicationScope
import dev.hridaya.kubenexus.core.nativebridge.KubeNexusNativeBridge
import dev.hridaya.kubenexus.domain.repository.ClusterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp(Application::class)
class KubeNexusApp : Hilt_KubeNexusApp() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var nativeBridge: KubeNexusNativeBridge

    @Inject
    lateinit var clusterRepository: ClusterRepository

    override fun onCreate() {
        super.onCreate()
        nativeBridge.initialize()

        // Migrate any legacy plaintext cluster kubeconfigs to Keystore-backed encryption
        applicationScope.launch {
            clusterRepository.migratePlaintextClusters()
        }
    }
}
