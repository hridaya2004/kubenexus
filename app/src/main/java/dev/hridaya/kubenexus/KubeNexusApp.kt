package dev.hridaya.kubenexus

import android.app.Application
import dev.hridaya.kubenexus.core.di.AppContainer
import dev.hridaya.kubenexus.core.di.DefaultAppContainer

class KubeNexusApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}
