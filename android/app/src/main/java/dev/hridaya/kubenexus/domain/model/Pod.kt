package dev.hridaya.kubenexus.domain.model

enum class PodStatus(val title: String) {
    RUNNING("Running"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CRASH_LOOP("CrashLoopBackOff"),
    UNKNOWN("Unknown"),
}

data class Pod(
    val id: String,
    val name: String,
    val namespace: String,
    val status: PodStatus = PodStatus.RUNNING,
    val readyContainers: String = "1/1",
    val restarts: Int = 0,
    val age: String = "1d",
    val ip: String? = null,
    val node: String? = null,
    val image: String? = null,
)
