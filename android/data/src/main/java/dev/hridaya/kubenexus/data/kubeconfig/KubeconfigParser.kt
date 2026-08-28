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
        val clusterName = extractClusterName(trimmed, currentContext)
        val clusterBlock = extractClusterBlock(trimmed, clusterName)
        val serverUrl = extractServerUrl(clusterBlock, trimmed)
        val userName = extractUserName(trimmed, currentContext)
        val namespace = extractNamespace(trimmed, currentContext) ?: "default"
        val caData = extractCertificateAuthorityData(clusterBlock, trimmed)
        val insecureSkip = extractInsecureSkipTlsVerify(clusterBlock, trimmed)

        require(serverUrl.isNotBlank()) {
            "No Kubernetes API server URL found in kubeconfig. Please verify the 'clusters.cluster.server' field."
        }

        val finalClusterName = when {
            !customName.isNullOrBlank() -> customName.trim()
            clusterName.isNotBlank() -> clusterName
            currentContext.isNotBlank() -> currentContext
            else -> "k8s-cluster"
        }

        return ParsedKubeconfig(
            clusterName = finalClusterName,
            serverUrl = serverUrl,
            contextName = if (currentContext.isNotBlank()) currentContext else finalClusterName,
            userName = userName,
            namespace = namespace,
            rawKubeconfig = trimmed,
            certificateAuthorityData = caData,
            insecureSkipTlsVerify = insecureSkip,
        )
    }

    private fun extractValue(content: String, key: String): String {
        val regex =
            Regex("""(?:^|\n)\s*$key\s*:\s*["']?([^"'\r\n#]+)["']?""", RegexOption.IGNORE_CASE)
        val match = regex.find(content)
        return match?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractClusterBlock(content: String, clusterName: String): String? {
        if (clusterName.isNotBlank()) {
            // Pattern 1: - cluster: ... name: clusterName
            val p1 = Regex(
                """-[ \t]*cluster:\s*([\s\S]*?)name:\s*["']?${Regex.escape(clusterName)}["']?""",
                RegexOption.IGNORE_CASE,
            )
            val m1 = p1.find(content)
            if (m1 != null) {
                return m1.groupValues[1]
            }

            // Pattern 2: - name: clusterName ... cluster: ...
            val p2 = Regex(
                """-[ \t]*name:\s*["']?${Regex.escape(clusterName)}["']?[\s\S]*?cluster:\s*([\s\S]*?)(?=\n[ \t]*-[ \t]*|\n[a-zA-Z0-9_-]+:|\Z)""",
                RegexOption.IGNORE_CASE,
            )
            val m2 = p2.find(content)
            if (m2 != null) {
                return m2.groupValues[1]
            }
        }
        return null
    }

    private fun extractServerUrl(clusterBlock: String?, content: String): String {
        val serverRegex =
            Regex("""(?:^|\n)\s*server\s*:\s*["']?([^"'\r\n#\s]+)["']?""", RegexOption.IGNORE_CASE)
        if (clusterBlock != null) {
            val match = serverRegex.find(clusterBlock)
            if (match != null) return match.groupValues[1].trim()
        }
        val match = serverRegex.find(content)
        return match?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractCertificateAuthorityData(clusterBlock: String?, content: String): String? {
        val caRegex = Regex(
            """certificate-authority-data\s*:\s*["']?([^"'\r\n#\s]+)["']?""",
            RegexOption.IGNORE_CASE,
        )
        if (clusterBlock != null) {
            val match = caRegex.find(clusterBlock)
            if (match != null) {
                val data = match.groupValues[1].trim()
                if (data.isNotBlank()) return data
            }
        }
        val match = caRegex.find(content)
        val data = match?.groupValues?.get(1)?.trim().orEmpty()
        return data.ifBlank { null }
    }

    private fun extractInsecureSkipTlsVerify(clusterBlock: String?, content: String): Boolean {
        val insecureRegex = Regex(
            """insecure-skip-tls-verify\s*:\s*["']?(true|false)["']?""",
            RegexOption.IGNORE_CASE,
        )
        if (clusterBlock != null) {
            val match = insecureRegex.find(clusterBlock)
            if (match != null) {
                return match.groupValues[1].trim().equals("true", ignoreCase = true)
            }
        }
        val match = insecureRegex.find(content)
        return match?.groupValues?.get(1)?.trim()?.equals("true", ignoreCase = true) ?: false
    }

    private fun extractClusterName(content: String, currentContext: String): String {
        if (currentContext.isNotBlank()) {
            val contextCluster = extractClusterFromContext(content, currentContext)
            if (contextCluster.isNotBlank()) return contextCluster
        }
        val clusterNameRegex = Regex(
            """clusters:\s*(?:[\r\n]+[ \t]*-[ \t]*cluster:[\s\S]*?name:\s*["']?([^"'\r\n#]+)["']?|[\r\n]+[ \t]*-[ \t]*name:\s*["']?([^"'\r\n#]+)["']?)""",
            RegexOption.IGNORE_CASE,
        )
        val match = clusterNameRegex.find(content)
        val g1 = match?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val g2 = match?.groupValues?.getOrNull(2)?.trim().orEmpty()
        return g1.ifEmpty { g2 }
    }

    private fun extractClusterFromContext(content: String, contextName: String): String {
        val regex = Regex(
            """name:\s*["']?${Regex.escape(contextName)}["']?[\s\S]*?cluster:\s*["']?([^"'\r\n#\s]+)["']?""",
            RegexOption.IGNORE_CASE,
        )
        val match = regex.find(content)
        return match?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractUserName(content: String, currentContext: String): String {
        if (currentContext.isNotBlank()) {
            val regex = Regex(
                """name:\s*["']?${Regex.escape(currentContext)}["']?[\s\S]*?user:\s*["']?([^"'\r\n#\s]+)["']?""",
                RegexOption.IGNORE_CASE,
            )
            val match = regex.find(content)
            val user = match?.groupValues?.get(1)?.trim().orEmpty()
            if (user.isNotBlank()) return user
        }
        val userRegex =
            Regex("""(?:^|\n)\s*user\s*:\s*["']?([^"'\r\n#\s]+)["']?""", RegexOption.IGNORE_CASE)
        return userRegex.find(content)?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractNamespace(content: String, currentContext: String): String? {
        if (currentContext.isNotBlank()) {
            val regex = Regex(
                """name:\s*["']?${Regex.escape(currentContext)}["']?[\s\S]*?namespace:\s*["']?([^"'\r\n#\s]+)["']?""",
                RegexOption.IGNORE_CASE,
            )
            val match = regex.find(content)
            val ns = match?.groupValues?.get(1)?.trim().orEmpty()
            if (ns.isNotBlank()) return ns
        }
        return null
    }
}
