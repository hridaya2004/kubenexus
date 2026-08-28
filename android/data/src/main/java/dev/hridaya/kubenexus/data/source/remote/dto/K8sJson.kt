package dev.hridaya.kubenexus.data.source.remote.dto

import kotlinx.serialization.json.Json

/**
 * Shared JSON configuration for decoding Kubernetes objects.
 *
 * `ignoreUnknownKeys` is essential rather than merely convenient: the DTOs in
 * this package model only the fields the app renders, while the API server sends
 * the complete object. A newer Kubernetes version adding a field must not break
 * an installed client.
 *
 * `explicitNulls = false` keeps absent and null indistinguishable, matching how
 * Kubernetes omits empty optional fields.
 */
val K8sJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}
