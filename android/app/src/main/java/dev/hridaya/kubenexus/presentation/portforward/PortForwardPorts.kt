package dev.hridaya.kubenexus.presentation.portforward

import dev.hridaya.kubenexus.domain.model.ContainerDetail

/** Lowest port Android apps may usefully bind without root privileges. */
private const val MIN_LOCAL_PORT = 1024

private const val MAX_PORT = 65535

/**
 * A forwardable port advertised by one pod container, parsed from the
 * kubectl-style entries in [ContainerDetail.ports] ("80/TCP",
 * "30080:80/TCP", "0.0.0.0:30080->80/TCP").
 */
data class PodPortTarget(
    val containerName: String,
    val remotePort: Int,
    val protocol: String? = null,
    val hostPort: Int? = null,
) {
    /** Compact rendering of the remote side, e.g. "8080/TCP". */
    val remoteLabel: String
        get() = "$remotePort" + (protocol?.let { "/$it" } ?: "")

    /** Rendering including an optional node host port, e.g. "host 30080". */
    val hostHint: String?
        get() = hostPort?.let { "host $it" }
}

/**
 * Extracts every forwardable port across [containers], deduplicated by
 * (remotePort, hostPort) since several containers often expose the same port.
 */
fun podPortTargets(containers: List<ContainerDetail>): List<PodPortTarget> =
    containers
        .flatMap { container ->
            container.ports.mapNotNull { entry ->
                parsePodPortEntry(entry)?.let { (hostPort, remotePort, protocol) ->
                    PodPortTarget(
                        containerName = container.name,
                        remotePort = remotePort,
                        protocol = protocol,
                        hostPort = hostPort,
                    )
                }
            }
        }.distinctBy { it.remotePort to it.hostPort }

/** Returns (hostPort?, remotePort, protocol?) or null when unparseable. */
private fun parsePodPortEntry(entry: String): Triple<Int?, Int, String?>? {
    val trimmed = entry.trim()
    if (trimmed.isEmpty()) return null

    val (portPart, protocol) = trimmed.split('/', limit = 2).let { parts ->
        parts.first() to parts.getOrNull(1)?.uppercase()
    }

    // Docker-style "0.0.0.0:30080->80" reduces to its container side first.
    val containerSide = portPart.substringAfter("->")
    val remote = containerSide.substringAfterLast(':').trim().toIntOrNull()

    val hostPort = portPart
        .substringBefore("->")
        .substringAfterLast(':', missingDelimiterValue = "")
        .trim()
        .toIntOrNull()

    return remote?.takeIf { it in 1..MAX_PORT }?.let { Triple(hostPort, it, protocol) }
}

/** Suggested local port: remote plus 2000, skipping any already taken local ports. */
fun defaultLocalPort(remotePort: Int, takenPorts: Set<Int> = emptySet()): Int {
    var candidate = (remotePort + 2000).coerceIn(MIN_LOCAL_PORT, MAX_PORT)
    while (candidate in takenPorts && candidate < MAX_PORT) {
        candidate++
    }
    return candidate
}

/**
 * Validates a local port text field. Returns null when acceptable, otherwise a
 * user-facing message. [takenLocalPorts] holds ports already forwarded on this
 * screen; binding the same local port twice can never succeed.
 */
fun validateLocalPort(rawInput: String, takenLocalPorts: Set<Int>): String? {
    val text = rawInput.trim()
    if (text.isEmpty()) return "Enter a local port"
    val port = text.toIntOrNull() ?: return "Port must be a whole number"
    return when {
        port < MIN_LOCAL_PORT || port > MAX_PORT ->
            "Port must be between $MIN_LOCAL_PORT and $MAX_PORT"

        port in takenLocalPorts -> "Port $port is already forwarded"

        else -> null
    }
}
