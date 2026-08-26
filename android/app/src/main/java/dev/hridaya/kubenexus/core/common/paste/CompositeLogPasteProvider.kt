package dev.hridaya.kubenexus.core.common.paste

import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result

/**
 * Composite [LogPasteProvider] that attempts upload through a list of delegate providers in priority order.
 * If the primary provider fails, it gracefully falls back to subsequent providers.
 */
class CompositeLogPasteProvider(
    private val providers: List<LogPasteProvider> = listOf(
        DpasteLogPasteProvider(),
        PasteRsLogPasteProvider(),
    ),
) : LogPasteProvider {

    override val name: String = "Composite (${providers.joinToString { it.name }})"

    override suspend fun upload(content: String, title: String?): Result<String> {
        if (content.isBlank()) {
            return Result.Error(AppError.Validation("Cannot export empty logs"))
        }
        if (providers.isEmpty()) {
            return Result.Error(AppError.Network("No paste providers configured"))
        }

        var lastError: Result.Error? = null
        for (provider in providers) {
            when (val result = provider.upload(content, title)) {
                is Result.Success -> return result
                is Result.Error -> lastError = result
                Result.Loading -> Unit
            }
        }
        return lastError ?: Result.Error(AppError.Network("All paste providers failed"))
    }
}
