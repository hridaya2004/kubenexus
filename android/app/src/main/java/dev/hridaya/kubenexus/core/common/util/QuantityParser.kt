package dev.hridaya.kubenexus.core.common.util

import kotlin.math.pow

/**
 * Parses Kubernetes resource quantities ("100m", "2501k", "128Mi") into plain
 * numbers. CPU quantities resolve to cores; memory quantities to bytes.
 */
object QuantityParser {

    private val CPU_SUFFIXES = mapOf(
        "n" to 1e-9,
        "u" to 1e-6,
        "m" to 1e-3,
        "" to 1.0,
        "k" to 1e3,
    )

    private val BINARY_SUFFIXES = mapOf(
        "" to 1.0,
        "Ki" to 1024.0,
        "Mi" to 1024.0.pow(2),
        "Gi" to 1024.0.pow(3),
        "Ti" to 1024.0.pow(4),
        "k" to 1e3,
        "K" to 1e3,
        "M" to 1e6,
        "G" to 1e9,
        "T" to 1e12,
    )

    fun parseCores(raw: String?): Double = parse(raw, CPU_SUFFIXES)

    fun parseBytes(raw: String?): Long = parse(raw, BINARY_SUFFIXES).toLong()

    private fun parse(raw: String?, suffixes: Map<String, Double>): Double {
        if (raw.isNullOrBlank()) return 0.0
        val value = raw.trim()
        val splitAt = value.indexOfFirst { !it.isDigit() && it != '.' && it != '-' }
        if (splitAt < 0) return value.toDoubleOrNull() ?: 0.0
        val number = value.take(splitAt).toDoubleOrNull() ?: return 0.0
        val multiplier = suffixes[value.substring(splitAt)] ?: return 0.0
        return number * multiplier
    }
}
