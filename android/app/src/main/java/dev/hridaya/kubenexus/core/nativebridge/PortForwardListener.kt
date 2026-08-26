package dev.hridaya.kubenexus.core.nativebridge

/**
 * Progress callbacks for a port-forward tunnel, mirroring how [ExecCallback]
 * wraps long-running exec sessions: the Go core owns the connection and calls
 * back into Kotlin as its state changes.
 *
 * Every method receives [handleId][PortForwardListener] so one listener can
 * serve several concurrent forwards without closing over per-call state.
 *
 * Callbacks are invoked on Go-managed threads; implementors must hop to their
 * own dispatcher before touching UI state.
 */
interface PortForwardListener {
    /**
     * The tunnel accepts connections on localhost at [localPort]. Until this
     * fires, binding succeeded but the upstream pod connection may not exist.
     */
    fun onPortForwardReady(handleId: String, localPort: Int)

    /** The tunnel failed after start (dial error, pod gone, port busy). */
    fun onPortForwardError(handleId: String, message: String)

    /** The tunnel closed; [reason] distinguishes user stop from remote close. */
    fun onPortForwardStopped(handleId: String, reason: String)
}
