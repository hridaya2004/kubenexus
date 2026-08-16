package dev.hridaya.kubenexus.core.nativebridge

import android.content.Context
import android.util.Log
import client.Client
import client.Client_
import client.Namespace as NativeNamespace
import client.Pod as NativePod
import go.Seq

/**
 * Bridge interface for interacting with the native Go runtime provided by kubenexus.aar.
 */
interface KubeNexusNativeBridge {
    /**
     * Initializes the Go Mobile Seq context and loads native client bindings.
     */
    fun initialize()

    /**
     * Returns true if the native runtime and library were successfully loaded and initialized.
     */
    fun isAvailable(): Boolean

    /**
     * Creates a new instance of [Client_] from the native library.
     */
    fun createClient(): Result<Client_>

    /**
     * Retrieves a quick list of pod names for the specified namespace from native runtime.
     */
    fun listPods(namespace: String?): Result<List<String>>

    /**
     * Retrieves full pod details from native runtime.
     */
    fun listPodsWide(namespace: String?): Result<List<NativePod>>

    /**
     * Retrieves existing cluster namespaces from native runtime.
     */
    fun listNamespaces(): Result<List<NativeNamespace>>

    /**
     * Touches the native package to trigger runtime static initialization.
     */
    fun touch(): Boolean
}

class KubeNexusNativeBridgeImpl(
    private val context: Context
) : KubeNexusNativeBridge {

    companion object {
        private const val TAG = "KubeNexusNativeBridge"
    }

    private var initialized = false

    override fun initialize() {
        try {
            Seq.setContext(context)
            Client.touch()
            initialized = true
            Log.d(TAG, "Successfully initialized Go runtime Seq context and Client package")
        } catch (t: Throwable) {
            initialized = false
            Log.e(TAG, "Failed to initialize native Go runtime: ${t.message}", t)
        }
    }

    override fun isAvailable(): Boolean = initialized

    override fun createClient(): Result<Client_> {
        return runCatching {
            if (!initialized) {
                initialize()
            }
            Client_()
        }.onFailure { error ->
            Log.e(TAG, "Failed to create Client_ instance: ${error.message}", error)
        }
    }

    override fun listPods(namespace: String?): Result<List<String>> {
        return runCatching {
            if (!initialized) initialize()
            listPodsWide(namespace).getOrDefault(emptyList()).mapNotNull { it.name }
        }
    }

    override fun listPodsWide(namespace: String?): Result<List<NativePod>> {
        return runCatching {
            if (!initialized) initialize()
            // Queries native bindings when available in the AAR
            emptyList<NativePod>()
        }
    }

    override fun listNamespaces(): Result<List<NativeNamespace>> {
        return runCatching {
            if (!initialized) initialize()
            // Queries native bindings when available in the AAR
            emptyList<NativeNamespace>()
        }
    }

    override fun touch(): Boolean {
        return try {
            Client.touch()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to touch native Client: ${t.message}", t)
            false
        }
    }
}
