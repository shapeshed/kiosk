package com.shapeshed.kiosk.ui

import java.io.BufferedInputStream
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request

internal fun String.pdfUrlOrNull(): String? {
    val uri = runCatching { java.net.URI(this) }.getOrNull() ?: return null
    val path = uri.path.orEmpty()
    if (!path.substringAfterLast('/').endsWith(".pdf", ignoreCase = true)) return null

    if (uri.host.equals("github.com", ignoreCase = true)) {
        val segments = path.trim('/').split('/')
        val blobIndex = segments.indexOf("blob")
        if (blobIndex == 2 && segments.size > 4) {
            val owner = segments[0]
            val repo = segments[1]
            val ref = segments[3]
            val filePath = segments.drop(4).joinToString("/")
            return "https://raw.githubusercontent.com/$owner/$repo/$ref/$filePath"
        }
    }

    return this
}

internal fun String.requiresExternalApp(): Boolean {
    val host = runCatching { java.net.URI(this).host.orEmpty() }.getOrDefault("")
        .removePrefix("www.")
        .removePrefix("mobile.")
        .removePrefix("m.")
    return host == "twitter.com" ||
        host == "x.com" ||
        host == "youtube.com" ||
        host == "youtu.be"
}

internal fun OkHttpClient.downloadPdf(url: String, directory: File): File {
    directory.mkdirs()
    val target = File(directory, "${url.hashCode().toUInt()}.pdf")
    if (target.exists() && target.hasPdfMagicHeader()) return target
    if (target.exists()) target.delete()
    newCall(Request.Builder().url(url).build()).execute().use { response ->
        if (!response.isSuccessful) error("Could not download PDF: ${response.code}")
        val body = response.body
        val contentType = body.contentType()
        val mimeLooksPdf = contentType?.type == "application" && contentType.subtype.equals("pdf", ignoreCase = true)
        target.outputStream().use { output ->
            BufferedInputStream(body.byteStream()).use { input ->
                val header = ByteArray(PdfMagicHeader.size)
                val bytesRead = input.read(header)
                val bodyLooksPdf = bytesRead == PdfMagicHeader.size && header.contentEquals(PdfMagicHeader)
                if (!mimeLooksPdf && !bodyLooksPdf) error("Response is not a PDF")
                output.write(header, 0, bytesRead.coerceAtLeast(0))
                input.copyTo(output)
            }
        }
    }
    return target
}

private val PdfMagicHeader = "%PDF-".encodeToByteArray()

private fun File.hasPdfMagicHeader(): Boolean =
    runCatching {
        inputStream().use { input ->
            val header = ByteArray(PdfMagicHeader.size)
            input.read(header) == PdfMagicHeader.size && header.contentEquals(PdfMagicHeader)
        }
    }.getOrDefault(false)
