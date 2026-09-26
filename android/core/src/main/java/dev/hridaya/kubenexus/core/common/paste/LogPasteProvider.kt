package dev.hridaya.kubenexus.core.common.paste

import dev.hridaya.kubenexus.core.common.result.Result

/**
 * A remote paste service that pod logs can be uploaded to.
 *
 * Uploads leave the device and land on a third-party service, so every property the user
 * must be told before agreeing to an upload is part of the contract.
 */
interface LogPasteProvider {
    /** Host the logs are sent to, shown to the user before uploading (e.g. "dpaste.org"). */
    val name: String

    /** How long the service keeps an upload, in words (e.g. "7 days"). */
    val retention: String

    /**
     * Uploads [content] and returns the public URL of the paste. [content] is sent as given;
     * callers are responsible for redacting it first.
     */
    suspend fun upload(content: String): Result<String>
}
