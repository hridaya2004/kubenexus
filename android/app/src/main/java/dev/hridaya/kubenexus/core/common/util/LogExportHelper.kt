package dev.hridaya.kubenexus.core.common.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import dev.hridaya.kubenexus.core.common.paste.CompositeLogPasteProvider
import dev.hridaya.kubenexus.core.common.paste.LogPasteProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility for exporting and sharing pod logs as files or to pluggable pastebin services.
 */
object LogExportHelper {

    private var defaultProvider: LogPasteProvider = CompositeLogPasteProvider()

    /**
     * Sets the active or default [LogPasteProvider] used for remote log uploads.
     */
    fun setDefaultProvider(provider: LogPasteProvider) {
        defaultProvider = provider
    }

    /**
     * Returns the currently configured default [LogPasteProvider].
     */
    fun getDefaultProvider(): LogPasteProvider = defaultProvider

    /**
     * Shares logs as a text file payload using Android's native share sheet.
     */
    fun shareAsFile(
        context: Context,
        content: String,
        filename: String = "pod.log",
    ) {
        if (content.isBlank()) {
            Toast.makeText(context, "No logs to export", Toast.LENGTH_SHORT).show()
            return
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, filename)
            putExtra(Intent.EXTRA_TITLE, filename)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooser = Intent.createChooser(sendIntent, "Export Logs ($filename)")
        context.startActivity(chooser)
    }

    /**
     * Uploads the log content using the provided (or default) [LogPasteProvider].
     */
    suspend fun uploadToPastebin(
        content: String,
        title: String? = null,
        provider: LogPasteProvider = defaultProvider,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Result<String> = withContext(dispatcher) {
        if (content.isBlank()) {
            return@withContext Result.Error(
                AppError.Validation("Cannot export empty logs"),
            )
        }
        provider.upload(content, title)
    }

    /**
     * Copies a string to clipboard and displays a confirmation toast.
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Logs") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
