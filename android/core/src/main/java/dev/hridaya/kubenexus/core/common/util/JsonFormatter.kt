package dev.hridaya.kubenexus.core.common.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Utility for detecting and pretty-printing structured JSON strings (objects and arrays).
 * Used to identify whether label or annotation values in Kubernetes resources are in JSON format
 * and format them with clean indentation.
 */
object JsonFormatter {
    private val prettyJson = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    /**
     * Determines whether [raw] is structured JSON (object or array).
     * If valid, returns the pretty-printed JSON string with 2-space indentation.
     * Otherwise returns null.
     */
    fun formatIfJson(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.length < 2) return null
        val isObject = trimmed.startsWith('{') && trimmed.endsWith('}')
        val isArray = trimmed.startsWith('[') && trimmed.endsWith(']')
        if (!isObject && !isArray) return null

        return try {
            val element = prettyJson.parseToJsonElement(trimmed)
            if (element is JsonObject || element is JsonArray) {
                prettyJson.encodeToString(JsonElement.serializer(), element)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if [raw] is structured JSON (object or array).
     */
    fun isJson(raw: String): Boolean = formatIfJson(raw) != null
}
