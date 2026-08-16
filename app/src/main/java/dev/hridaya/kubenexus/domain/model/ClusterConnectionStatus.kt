package dev.hridaya.kubenexus.domain.model

enum class ClusterConnectionStatus(val title: String) {
    CONNECTED("Connected"),
    CONNECTING("Connecting"),
    DISCONNECTED("Disconnected"),
    OFFLINE("Offline")
}
