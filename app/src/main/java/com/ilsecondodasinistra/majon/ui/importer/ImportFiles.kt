package com.ilsecondodasinistra.majon.ui.importer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ilsecondodasinistra.majon.ui.projectdetail.SharedProject
import java.io.File

/** Reading a file the person chose or opened, and handing a project to the share sheet. */
object ImportFiles {

    /** More than this is not a pattern's notes or a project: refused rather than read into memory. */
    private const val MAX_BYTES = 4_000_000

    const val MIME_PROJECT = "application/x-majon"

    /** The text of the file at [uri], or null when it cannot be read or is too big. */
    fun readText(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_BYTES) return@use null
                out.write(buffer, 0, read)
            }
            out.toString(Charsets.UTF_8.name())
        }
    }.getOrNull()

    /** Writes the project where the share sheet can read it, and opens the sheet. */
    fun share(context: Context, shared: SharedProject, chooserTitle: String) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        // One file at a time: the older ones are no use any more
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, shared.fileName).apply { writeText(shared.content) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = MIME_PROJECT
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, shared.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
