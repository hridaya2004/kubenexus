package dev.hridaya.kubenexus.core.security

/**
 * Sanitizes log messages, error traces, and diagnostic output to prevent credentials,
 * tokens, certificates, and private keys from ever being written to logs or displayed in error traces.
 */
object LogSanitizer {

    private val TOKEN_REGEX = Regex("""(?i)(token\s*:\s*["']?)([^"'\r\n#\s]+)(["']?)""")
    private val PASSWORD_REGEX = Regex("""(?i)(password\s*:\s*["']?)([^"'\r\n#\s]+)(["']?)""")
    private val CLIENT_CERT_DATA_REGEX =
        Regex("""(?i)(client-certificate-data\s*:\s*["']?)([^"'\r\n#\s]+)(["']?)""")
    private val CLIENT_KEY_DATA_REGEX =
        Regex("""(?i)(client-key-data\s*:\s*["']?)([^"'\r\n#\s]+)(["']?)""")
    private val CERT_AUTHORITY_DATA_REGEX =
        Regex("""(?i)(certificate-authority-data\s*:\s*["']?)([^"'\r\n#\s]+)(["']?)""")
    private val BEARER_HEADER_REGEX = Regex("""(?i)(Bearer\s+)[A-Za-z0-9\-_.~+/]+=*""")
    private val BASIC_HEADER_REGEX = Regex("""(?i)(Basic\s+)[A-Za-z0-9+/]+=*""")
    private val PEM_BLOCK_REGEX =
        Regex("""-----BEGIN [A-Z0-9\s_-]+-----[\s\S]*?-----END [A-Z0-9\s_-]+-----""")
    private val URL_AUTH_REGEX = Regex("""(https?://[^:\s/]+):([^@\s/]+)@""")

    /**
     * Sanitizes the input string by replacing all known sensitive patterns with redacted placeholders.
     */
    fun sanitize(message: String?): String {
        if (message.isNullOrEmpty()) return ""
        var result = message
        result = TOKEN_REGEX.replace(result, "$1[REDACTED]$3")
        result = PASSWORD_REGEX.replace(result, "$1[REDACTED]$3")
        result = CLIENT_CERT_DATA_REGEX.replace(result, "$1[REDACTED]$3")
        result = CLIENT_KEY_DATA_REGEX.replace(result, "$1[REDACTED]$3")
        result = CERT_AUTHORITY_DATA_REGEX.replace(result, "$1[REDACTED]$3")
        result = BEARER_HEADER_REGEX.replace(result, "$1[REDACTED]")
        result = BASIC_HEADER_REGEX.replace(result, "$1[REDACTED]")
        result = PEM_BLOCK_REGEX.replace(result, "[REDACTED PEM BLOCK]")
        result = URL_AUTH_REGEX.replace(result, "$1:[REDACTED]@")
        return result
    }
}
