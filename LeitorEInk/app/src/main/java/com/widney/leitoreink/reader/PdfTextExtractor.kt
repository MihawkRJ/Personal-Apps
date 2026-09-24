/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.reader

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/**
 * Extrai o texto de um PDF para o "modo texto" do leitor, em que o conteúdo é
 * exibido como texto corrido e as linhas se reorganizam sozinhas ao aumentar a
 * fonte (em vez de dar zoom na imagem da página e cortar as bordas).
 *
 * Funciona com PDFs que têm texto de verdade. PDFs digitalizados (páginas que
 * são só imagem) não têm texto para extrair — nesse caso o leitor avisa e
 * continua no modo página.
 */
class PdfTextExtractor private constructor(private val document: PDDocument) : AutoCloseable {

    val pageCount: Int get() = try { document.numberOfPages } catch (e: Exception) { 0 }

    /** Texto da página [index] (0-based), já remontado em parágrafos. */
    fun textForPage(index: Int): String? {
        return try {
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            stripper.startPage = index + 1
            stripper.endPage = index + 1
            val raw = stripper.getText(document)
            if (raw.isNullOrBlank()) null else reflow(raw)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /**
     * O PDF guarda cada linha visual separadamente; para o texto poder "refluir"
     * na tela, juntamos as linhas de um mesmo parágrafo numa linha só, desfazendo
     * a hifenização de fim de linha. Um parágrafo novo começa quando a linha
     * anterior termina em pontuação de fim de frase e é visivelmente mais curta
     * que as demais (típico de última linha de parágrafo).
     */
    private fun reflow(raw: String): String {
        val lines = raw.replace("\r\n", "\n").replace('\r', '\n').split("\n")
        val longest = lines.maxOfOrNull { it.trim().length } ?: 0
        val shortLineLimit = (longest * 0.72f)

        val result = StringBuilder()
        var previousLine = ""

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                if (result.isNotEmpty() && !result.endsWith("\n\n")) result.append("\n\n")
                previousLine = ""
                continue
            }

            val startsNewParagraph = result.isEmpty() ||
                result.endsWith("\n\n") ||
                (endsSentence(previousLine) && previousLine.length < shortLineLimit)

            when {
                startsNewParagraph -> {
                    if (result.isNotEmpty() && !result.endsWith("\n\n")) result.append("\n\n")
                    result.append(trimmed)
                }
                result.endsWith("-") -> {
                    result.setLength(result.length - 1)
                    result.append(trimmed)
                }
                else -> result.append(' ').append(trimmed)
            }
            previousLine = trimmed
        }

        return result.toString().trim()
    }

    private fun endsSentence(line: String): Boolean {
        if (line.isEmpty()) return false
        return line.last() in charArrayOf('.', '!', '?', ':', '"', '”', '»', '…')
    }

    override fun close() {
        try {
            document.close()
        } catch (_: Exception) {
        }
    }

    companion object {
        fun open(context: Context, uri: Uri): PdfTextExtractor? {
            return try {
                val document = context.contentResolver.openInputStream(uri)?.use { input ->
                    PDDocument.load(input)
                } ?: return null
                PdfTextExtractor(document)
            } catch (e: Exception) {
                null
            } catch (e: OutOfMemoryError) {
                null
            }
        }
    }
}
