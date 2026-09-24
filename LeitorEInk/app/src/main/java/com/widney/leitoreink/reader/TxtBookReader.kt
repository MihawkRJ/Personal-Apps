/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.reader

import android.content.Context
import android.net.Uri

/**
 * Leitor de TXT simples: carrega o arquivo inteiro em memória (arquivos .txt de
 * livro raramente passam de poucos MB) e o divide em "páginas" de tamanho fixo
 * em caracteres, cortando em espaços/quebras de linha para não partir palavras.
 */
class TxtBookReader private constructor(private val fullText: String) {

    private var pages: List<String> = emptyList()

    var charsPerPage: Int = DEFAULT_CHARS_PER_PAGE
        private set

    val pageCount: Int get() = pages.size

    init {
        paginate(DEFAULT_CHARS_PER_PAGE)
    }

    fun page(index: Int): String? = pages.getOrNull(index)

    /**
     * Repagina o texto com um novo tamanho de "página" (usado ao mudar o tamanho da
     * fonte). Retorna o índice de página que contém aproximadamente o mesmo trecho
     * que estava sendo lido antes, para não perder a posição de leitura.
     */
    fun repaginate(newCharsPerPage: Int, currentPageIndex: Int): Int {
        val currentOffset = pages.take(currentPageIndex.coerceIn(0, pages.size)).sumOf { it.length }
        paginate(newCharsPerPage)

        var offset = 0
        for ((idx, p) in pages.withIndex()) {
            offset += p.length
            if (offset >= currentOffset) return idx
        }
        return (pages.size - 1).coerceAtLeast(0)
    }

    private fun paginate(charsPerPage: Int) {
        this.charsPerPage = charsPerPage.coerceAtLeast(200)
        val result = mutableListOf<String>()
        val length = fullText.length
        var start = 0

        while (start < length) {
            var end = (start + this.charsPerPage).coerceAtMost(length)
            if (end < length) {
                val searchFrom = (end - 1).coerceIn(start, length - 1)
                val breakPoint = fullText.lastIndexOfAny(charArrayOf(' ', '\n', '\t'), searchFrom)
                if (breakPoint > start) {
                    end = breakPoint + 1
                }
            }
            result.add(fullText.substring(start, end))
            start = end
        }
        pages = if (result.isEmpty()) listOf("") else result
    }

    companion object {
        const val DEFAULT_CHARS_PER_PAGE = 1800

        fun open(context: Context, uri: Uri): TxtBookReader? {
            return try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return null
                TxtBookReader(decodeText(bytes))
            } catch (e: Exception) {
                null
            }
        }

        /** Tenta UTF-8; se parecer estar errado (muitos caracteres inválidos), tenta Latin-1. */
        private fun decodeText(bytes: ByteArray): String {
            val utf8 = String(bytes, Charsets.UTF_8)
            val invalidCount = utf8.count { it == '�' }
            return if (bytes.isNotEmpty() && invalidCount > bytes.size / 100) {
                String(bytes, Charsets.ISO_8859_1)
            } else {
                utf8
            }
        }
    }
}
