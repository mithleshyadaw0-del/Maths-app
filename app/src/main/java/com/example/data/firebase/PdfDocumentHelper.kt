package com.example.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.StudyNotePdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Generates real Android PDF documents for study sheets, downloads hosted PDFs via OkHttp,
 * renders multi-page previews using native PdfRenderer, and copies PDFs to SAF URIs
 * without requiring any broad storage permissions.
 */
object PdfDocumentHelper {
    private val httpClient = OkHttpClient()

    suspend fun preparePdfFile(context: Context, note: StudyNotePdf): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val safeName = note.id.ifBlank { note.title.hashCode().toString() }
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val targetFile = File(cacheDir, "${safeName}.pdf")

            if (note.pdfUrl.startsWith("http://") || note.pdfUrl.startsWith("https://")) {
                val request = Request.Builder().url(note.pdfUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful && response.body != null) {
                        val bytes = response.body!!.bytes()
                        if (bytes.size >= 4 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte()) {
                            FileOutputStream(targetFile).use { it.write(bytes) }
                            return@withContext Result.success(targetFile)
                        }
                    }
                }
            }

            // Generate a clean, branded multi-page PDF document from note metadata + contentMarkdown
            generateBrandedStudyPdf(targetFile, note)
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateBrandedStudyPdf(outputFile: File, note: StudyNotePdf) {
        val pdf = PdfDocument()
        val pageWidth = 595 // A4 width in points (72 dpi)
        val pageHeight = 842 // A4 height
        val margin = 42f
        val contentWidth = (pageWidth - margin * 2).toInt()

        val navyPaint = Paint().apply {
            color = Color.parseColor("#171936")
            style = Paint.Style.FILL
        }
        val goldPaint = Paint().apply {
            color = Color.parseColor("#FFB323")
            style = Paint.Style.FILL
        }
        val headerBrandPaint = Paint().apply {
            color = Color.parseColor("#FFB323")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val metaPaint = Paint().apply {
            color = Color.parseColor("#D8D9EE")
            textSize = 11f
            isAntiAlias = true
        }
        val bodyTextPaint = TextPaint().apply {
            color = Color.parseColor("#1D2038")
            textSize = 12.5f
            isAntiAlias = true
        }
        val sectionTitlePaint = TextPaint().apply {
            color = Color.parseColor("#171936")
            textSize = 14.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            color = Color.parseColor("#6E718A")
            textSize = 10f
            isAntiAlias = true
        }

        val rawLines = buildList {
            if (note.description.isNotBlank()) {
                add("Overview: ${note.description}")
                add("")
            }
            val body = note.contentMarkdown.ifBlank {
                "Chapter: ${note.chapter}\nSubject: ${note.subject}\nCategory: ${note.category}\n\n" +
                    "Key study notes prepared by Mithlesh Sir for Maths By Mithlesh Sir students."
            }
            addAll(body.lines())
        }

        try {
            var pageNumber = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas

            // Top Navy Banner on page 1
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 112f, navyPaint)
            canvas.drawRect(0f, 112f, pageWidth.toFloat(), 117f, goldPaint)
            canvas.drawText("MBS • MATHS BY MITHLESH SIR (सीखें गणित, बढ़ें सफलता की ओर)", margin, 34f, headerBrandPaint)
            canvas.drawText(note.title.take(48), margin, 64f, headerTitlePaint)
            canvas.drawText(
                "${note.category}  •  ${note.subject}  •  ${note.chapter}  •  ${note.materialType}",
                margin,
                90f,
                metaPaint
            )

            var y = 142f
            val maxY = pageHeight - 58f

            for (rawLine in rawLines) {
                val isHeading = rawLine.trim().startsWith("#")
                val cleanLine = rawLine.trim().removePrefix("###").removePrefix("##").removePrefix("#").trim()
                if (cleanLine.isEmpty()) {
                    y += 10f
                    continue
                }
                val paintToUse = if (isHeading) sectionTitlePaint else bodyTextPaint
                val staticLayout = StaticLayout.Builder.obtain(
                    cleanLine,
                    0,
                    cleanLine.length,
                    paintToUse,
                    contentWidth
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(4f, 1.15f)
                    .build()

                if (y + staticLayout.height > maxY) {
                    canvas.drawText(
                        "Maths By Mithlesh Sir • Free Study Material • Page $pageNumber",
                        margin,
                        pageHeight - 24f,
                        footerPaint
                    )
                    pdf.finishPage(page)
                    pageNumber++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, goldPaint)
                    y = 42f
                }

                canvas.save()
                canvas.translate(margin, y)
                staticLayout.draw(canvas)
                canvas.restore()
                y += staticLayout.height + if (isHeading) 10f else 6f
            }

            canvas.drawText(
                "Maths By Mithlesh Sir • Free Study Material • Page $pageNumber",
                margin,
                pageHeight - 24f,
                footerPaint
            )
            pdf.finishPage(page)

            FileOutputStream(outputFile).use { out ->
                pdf.writeTo(out)
            }
        } finally {
            pdf.close()
        }
    }

    suspend fun renderPdfPages(file: File, maxPages: Int = 12): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        try {
            val bitmaps = mutableListOf<Bitmap>()
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = minOf(renderer.pageCount, maxPages)
                    for (i in 0 until count) {
                        renderer.openPage(i).use { page ->
                            val scale = 2
                            val bmp = Bitmap.createBitmap(
                                page.width * scale,
                                page.height * scale,
                                Bitmap.Config.ARGB_8888
                            )
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bmp)
                        }
                    }
                }
            }
            Result.success(bitmaps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyFileToSafUri(context: Context, sourceFile: File, targetUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: error("Unable to open destination file stream.")
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
