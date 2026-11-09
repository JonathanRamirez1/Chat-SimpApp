package com.jonathan.chatsimpapp.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File

/** Convierte el acta en un PDF tamaño carta y abre el menú para compartirlo (función Pro). */
object ActaPdf {
    private const val PAGE_W = 612
    private const val PAGE_H = 792
    private const val MARGIN = 54f
    private const val LINE = 16f

    fun create(context: Context, text: String, name: String): File {
        val doc = PdfDocument()
        val body = Paint().apply { textSize = 11f; isAntiAlias = true }
        val title = Paint(body).apply { textSize = 15f; typeface = Typeface.DEFAULT_BOLD }
        val maxWidth = PAGE_W - 2 * MARGIN

        val lines = text.lines().flatMap { wrap(it, body, maxWidth) }
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
        var y = MARGIN
        lines.forEachIndexed { i, line ->
            if (y > PAGE_H - MARGIN) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNumber).create())
                y = MARGIN
            }
            page.canvas.drawText(line, MARGIN, y, if (i == 0) title else body)
            y += if (i == 0) LINE * 1.6f else LINE
        }
        doc.finishPage(page)

        val dir = File(context.cacheDir, "actas").apply { mkdirs() }
        val file = File(dir, "acta-$name.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun share(context: Context, text: String, name: String) {
        val file = create(context, text, name)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    private fun wrap(line: String, paint: Paint, maxWidth: Float): List<String> {
        if (line.isEmpty()) return listOf("")
        val out = mutableListOf<String>()
        var current = StringBuilder()
        line.split(' ').forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) out += current.toString()
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) out += current.toString()
        return out
    }
}
