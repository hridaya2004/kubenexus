package dev.hridaya.kubenexus.domain.model

/**
 * The concrete pod-side destination a Service forward tunnels into.
 *
 * A Service has no listener of its own; `kubectl port-forward service/...`
 * resolves the same way: pick a ready endpoint from the Endpoints object and
 * dial that endpoint pod directly. [podName] is the real pod object name (the
 * Go core opens the tunnel on `pods/{name}/portforward`, which requires it),
 * and [podPort] is the already-resolved numeric port that pod listens on.
 */
data class ServiceForwardTarget(
    val podName: String,
    val podPort: Int,
)
