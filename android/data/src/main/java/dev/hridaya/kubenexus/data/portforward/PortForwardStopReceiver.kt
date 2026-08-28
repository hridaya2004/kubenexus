package dev.hridaya.kubenexus.data.portforward

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * BroadcastReceiver triggered from the ongoing Port Forward notification
 * "Stop All" action to gracefully terminate all active forwarding tunnels.
 */
class PortForwardStopReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PortForwardStopReceiverEntryPoint {
        fun sessionManager(): PortForwardSessionManager
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context?.applicationContext ?: return
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            PortForwardStopReceiverEntryPoint::class.java,
        )
        entryPoint.sessionManager().stopAll()
    }
}
