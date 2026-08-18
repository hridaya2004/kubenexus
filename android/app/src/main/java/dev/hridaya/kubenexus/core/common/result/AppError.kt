package dev.hridaya.kubenexus.core.common.result

sealed interface AppError {
    val message: String

    data class Network(
        override val message: String = "Network connection failed. Please check your connection.",
        val statusCode: Int? = null,
    ) : AppError

    data class Database(override val message: String = "Failed to access local database.") : AppError

    data class Validation(override val message: String) : AppError

    data class NotFound(override val message: String = "The requested resource was not found.") : AppError

    data class Unknown(override val message: String = "An unexpected error occurred.", val throwable: Throwable? = null) : AppError
}
