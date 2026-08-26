package dev.hridaya.kubenexus.core.common.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import dev.hridaya.kubenexus.core.common.paste.CompositeLogPasteProvider
import dev.hridaya.kubenexus.core.common.paste.LogPasteProvider
import dev.hridaya.kubenexus.core.common.result.AppError
import dev.hridaya.kubenexus.core.common.result.Result
import java.io.File
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
     * Writes logs to a file in the app's cache directory and shares it as an
     * actual file attachment via Android's native share sheet and [FileProvider].
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

        try {
            val logsDir = File(context.cacheDir, "logs").apply { mkdirs() }
            val logFile = File(logsDir, filename).apply {
                writeText(content, Charsets.UTF_8)
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile,
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, filename)
                putExtra(Intent.EXTRA_TITLE, filename)
                clipData = ClipData.newUri(context.contentResolver, filename, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Export Logs ($filename)").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Failed to export log file: ${e.localizedMessage ?: e.message}",
                Toast.LENGTH_LONG,
            ).show()
        }
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
