package dev.hridaya.kubenexus.core.common.paste

import dev.hridaya.kubenexus.core.common.result.Result

/**
 * Pluggable provider for uploading logs to a remote paste service.
 * Enables seamlessly swapping or adding alternative pastebin backends.
 */
interface LogPasteProvider {
    /** Human-readable provider name (e.g. "dpaste.org", "paste.rs"). */
    val name: String

    /**
     * Uploads the given [content] with an optional [title] and returns the resulting public URL.
     */
    suspend fun upload(content: String, title: String? = null): Result<String>
}
