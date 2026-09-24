/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor

/**
 * Abre um PDF (via URI SAF) e renderiza páginas individuais como bitmaps,
 * usando a API nativa do Android (sem bibliotecas externas).
 */
class PdfBookReader(context: Context, uri: Uri) : AutoCloseable {

    /** Retângulo de conteúdo da página, em frações de 0 a 1 (sem as margens em branco). */
    data class CropBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

    private val pfd: ParcelFileDescriptor? =
        try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            null
        }

    private val renderer: PdfRenderer? =
        try {
            pfd?.let { PdfRenderer(it) }
        } catch (e: Exception) {
            null
        }

    val isValid: Boolean get() = renderer != null

    val pageCount: Int get() = renderer?.pageCount ?: 0

    /**
     * Descobre onde começa e termina o conteúdo da página, ignorando as margens
     * em branco. Renderiza uma miniatura bem pequena só para analisar os pixels,
     * então é rápido. Retorna null se a página for quase toda branca ou se o
     * conteúdo ocupar praticamente a página inteira (aí não vale recortar).
     */
    fun detectContentBox(index: Int): CropBox? {
        val safeRenderer = renderer ?: return null
        if (index < 0 || index >= safeRenderer.pageCount) return null

        return try {
            safeRenderer.openPage(index).use { page ->
                val thumbWidth = 160
                val thumbHeight = (thumbWidth * page.height.toFloat() / page.width.toFloat())
                    .toInt()
                    .coerceIn(1, 400)

                val thumb = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
                Canvas(thumb).drawColor(Color.WHITE)
                page.render(thumb, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pixels = IntArray(thumbWidth * thumbHeight)
                thumb.getPixels(pixels, 0, thumbWidth, 0, 0, thumbWidth, thumbHeight)
                thumb.recycle()

                var minX = thumbWidth
                var minY = thumbHeight
                var maxX = -1
                var maxY = -1

                for (y in 0 until thumbHeight) {
                    val rowOffset = y * thumbWidth
                    for (x in 0 until thumbWidth) {
                        val pixel = pixels[rowOffset + x]
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        if (r < 230 || g < 230 || b < 230) {
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                        }
                    }
                }

                if (maxX < 0 || maxY < 0) return@use null

                val padX = thumbWidth * 0.02f
                val padY = thumbHeight * 0.02f
                val left = ((minX - padX) / thumbWidth).coerceIn(0f, 1f)
                val top = ((minY - padY) / thumbHeight).coerceIn(0f, 1f)
                val right = ((maxX + 1 + padX) / thumbWidth).coerceIn(0f, 1f)
                val bottom = ((maxY + 1 + padY) / thumbHeight).coerceIn(0f, 1f)

                val usefulWidth = right - left
                val usefulHeight = bottom - top

                // Conteúdo pequeno demais (provável erro de detecção) ou quase a
                // página inteira (não há margem para ganhar): não recorta.
                if (usefulWidth < 0.3f || usefulHeight < 0.3f) return@use null
                if (usefulWidth > 0.97f && usefulHeight > 0.97f) return@use null

                CropBox(left, top, right, bottom)
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /**
     * Renderiza a página [index] (0-based) ajustada à largura [targetWidthPx].
     * Se [crop] for informado, renderiza só aquela região (o conteúdo sem as
     * margens), o que faz o texto aparecer maior sem cortar nada.
     * O fundo é sempre branco (páginas de PDF costumam ter fundo transparente).
     */
    fun renderPage(index: Int, targetWidthPx: Int, crop: CropBox? = null): Bitmap? {
        val safeRenderer = renderer ?: return null
        if (index < 0 || index >= safeRenderer.pageCount) return null

        return try {
            safeRenderer.openPage(index).use { page ->
                val pageWidth = page.width.toFloat()
                val pageHeight = page.height.toFloat()

                val cropLeft = (crop?.left ?: 0f) * pageWidth
                val cropTop = (crop?.top ?: 0f) * pageHeight
                val cropRight = (crop?.right ?: 1f) * pageWidth
                val cropBottom = (crop?.bottom ?: 1f) * pageHeight

                val cropWidth = (cropRight - cropLeft).coerceAtLeast(1f)
                val cropHeight = (cropBottom - cropTop).coerceAtLeast(1f)

                val width = targetWidthPx.coerceAtLeast(1)
                val scale = width / cropWidth
                val height = (cropHeight * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                val transform = Matrix()
                transform.setScale(scale, scale)
                transform.postTranslate(-cropLeft * scale, -cropTop * scale)

                page.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    override fun close() {
        try {
            renderer?.close()
        } catch (_: Exception) {
        }
        try {
            pfd?.close()
        } catch (_: Exception) {
        }
    }
}
