package dev.hridaya.kubenexus.data.kubeconfig

import dev.hridaya.kubenexus.domain.model.ParsedKubeconfig

object KubeconfigParser {

    /**
     * Parses a raw YAML/JSON kubeconfig string and extracts cluster, context, and server metadata.
     * Throws [IllegalArgumentException] with descriptive messages if validation fails.
     */
    fun parse(rawContent: String, customName: String? = null): ParsedKubeconfig {
        val trimmed = rawContent.trim()
        require(trimmed.isNotBlank()) { "Kubeconfig content cannot be empty." }

        val currentContext = extractValue(trimmed, "current-context")
        val serverUrl = extractServerUrl(trimmed)
        val clusterName = extractClusterName(trimmed, currentContext)
        val userName = extractUserName(trimmed, currentContext)
        val namespace = extractNamespace(trimmed, currentContext) ?: "default"

        require(serverUrl.isNotBlank()) {
            "No Kubernetes API server URL found in kubeconfig. Please verify the 'clusters.cluster.server' field."
        }

        val finalClusterName = when {
            !customName.isNullOrBlank() -> customName.trim()
            currentContext.isNotBlank() -> currentContext
            clusterName.isNotBlank() -> clusterName
            else -> "k8s-cluster"
        }

        return ParsedKubeconfig(
            clusterName = finalClusterName,
            serverUrl = serverUrl,
            contextName = if (currentContext.isNotBlank()) currentContext else finalClusterName,
            userName = userName,
            namespace = namespace,
            rawKubeconfig = trimmed
        )
    }

    private fun extractValue(content: String, key: String): String {
        val regex = Regex("""(?:^|\n)\s*$key\s*:\s*["']?([^"'\r\n#]+)["']?""", RegexOption.IGNORE_CASE)
        val match = regex.find(content)
        return match?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractServerUrl(content: String): String {
        val regex = Regex("""(?:^|\n)\s*server\s*:\s*["']?([^"'\r\n#\s]+)["']?""", RegexOption.IGNORE_CASE)
        val match = regex.find(content)
        return match?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractClusterName(content: String, currentContext: String): String {
        if (currentContext.isNotBlank()) {
            val contextCluster = extractClusterFromContext(content, currentContext)
            if (contextCluster.isNotBlank()) return contextCluster
        }
        val clusterNameRegex = Regex("""clusters:\s*(?:[\r\n]+[ \t]*-[ \t]*cluster:[\s\S]*?name:\s*["']?([^"'\r\n#]+)["']?|[\r\n]+[ \t]*-[ \t]*name:\s*["']?([^"'\r\n#]+)["']?)""", RegexOption.IGNORE_CASE)
        val match = clusterNameRegex.find(content)
        val g1 = match?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val g2 = match?.groupValues?.getOrNull(2)?.trim().orEmpty()
        return g1.ifEmpty { g2 }
    }

    private fun extractClusterFromContext(content: String, contextName: String): String {
        val regex = Regex("""name:\s*["']?${Regex.escape(contextName)}["']?[\s\S]*?cluster:\s*["']?([^"'\r\n#\s]+)["']?""", RegexOption.IGNORE_CASE)
        val match = regex.find(content)
        return match?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractUserName(content: String, currentContext: String): String {
        if (currentContext.isNotBlank()) {
            val regex = Regex("""name:\s*["']?${Regex.escape(currentContext)}["']?[\s\S]*?user:\s*["']?([^"'\r\n#\s]+)["']?""", RegexOption.IGNORE_CASE)
            val match = regex.find(content)
            val user = match?.groupValues?.get(1)?.trim().orEmpty()
            if (user.isNotBlank()) return user
        }
        val userRegex = Regex("""(?:^|\n)\s*user\s*:\s*["']?([^"'\r\n#\s]+)["']?""", RegexOption.IGNORE_CASE)
        return userRegex.find(content)?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractNamespace(content: String, currentContext: String): String? {
        if (currentContext.isNotBlank()) {
            val regex = Regex("""name:\s*["']?${Regex.escape(currentContext)}["']?[\s\S]*?namespace:\s*["']?([^"'\r\n#\s]+)["']?""", RegexOption.IGNORE_CASE)
            val match = regex.find(content)
            val ns = match?.groupValues?.get(1)?.trim().orEmpty()
            if (ns.isNotBlank()) return ns
        }
        return null
    }
}
