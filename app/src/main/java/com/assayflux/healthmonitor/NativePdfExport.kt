package com.assayflux.healthmonitor

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Native Android PDF writer. The caller supplies already-authorized report rows. */
object NativePdfExport {
    fun write(context: Context, uri: Uri, title: String, sections: List<Pair<String, List<String>>>) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 19f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = 0xff666666.toInt() }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun startPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            y = 48f
            page!!.canvas.drawText(title, margin, y, titlePaint)
            y += 18f
            page!!.canvas.drawText("Generated ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", margin, y, mutedPaint)
            y += 26f
        }
        fun finishPage() {
            val p = page ?: return
            p.canvas.drawText("Health Monitor · Private local report · Page $pageNumber", margin, 818f, mutedPaint)
            document.finishPage(p)
            page = null
        }
        fun line(text: String, paint: Paint, indent: Float = 0f) {
            val maxWidth = pageWidth - margin * 2 - indent
            val words = text.replace("\n", " ").split(Regex("\\s+"))
            var current = ""
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                    if (y > 785f) { finishPage(); startPage() }
                    page!!.canvas.drawText(current, margin + indent, y, paint)
                    y += if (paint.textSize >= 12f) 17f else 13f
                    current = word
                } else current = candidate
            }
            if (current.isNotEmpty()) {
                if (y > 785f) { finishPage(); startPage() }
                page!!.canvas.drawText(current, margin + indent, y, paint)
                y += if (paint.textSize >= 12f) 17f else 13f
            }
        }
        try {
            startPage()
            line("Confidential health information. Keep this document private.", mutedPaint)
            y += 8f
            sections.forEach { (heading, rows) ->
                if (y > 760f) { finishPage(); startPage() }
                line(heading, headingPaint)
                rows.forEach { line("• $it", bodyPaint, 8f) }
                y += 9f
            }
            finishPage()
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
                ?: throw IllegalStateException("Unable to open the selected PDF destination.")
        } finally { document.close() }
    }
}
