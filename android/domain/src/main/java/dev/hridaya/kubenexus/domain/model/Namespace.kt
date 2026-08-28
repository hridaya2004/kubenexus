package dev.hridaya.kubenexus.domain.model

/**
 * A cluster namespace.
 *
 * The status phase was previously discarded on the way through the bridge and
 * replaced with a hardcoded "Active" default when persisting, so a Terminating
 * namespace was indistinguishable from a healthy one. It is carried through now.
 */
data class Namespace(
    val name: String,
    val status: String = "Active",
    val age: String? = null,
)
