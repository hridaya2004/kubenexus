package dev.hridaya.kubenexus.data.portforward

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service that keeps Kubernetes port-forward TCP tunnels and native Go
 * worker threads alive when the app transitions into the background.
 */
class PortForwardService : Service() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PortForwardServiceEntryPoint {
        fun sessionManager(): PortForwardSessionManager
        fun notificationManager(): PortForwardNotificationManager
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PortForwardServiceEntryPoint::class.java,
        )
        val sessionManager = entryPoint.sessionManager()
        val notificationManager = entryPoint.notificationManager()

        // Immediate promotion to Foreground Service to satisfy Android timeout
        val initialSessions = sessionManager.sessions.value.filter { it.isActive }
        val initialNotification = notificationManager.buildNotification(initialSessions)

        ServiceCompat.startForeground(
            this,
            PortForwardNotificationManager.NOTIFICATION_ID,
            initialNotification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

        observeJob = serviceScope.launch {
            sessionManager.sessions.collect { list ->
                val activeSessions = list.filter { it.isActive }
                if (activeSessions.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    notificationManager.updateNotification(activeSessions)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        observeJob?.cancel()
        serviceScope.cancel()
    }
}
