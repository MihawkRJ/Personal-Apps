/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.scan

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.widney.leitoreink.data.BookFormat

/**
 * Varre pastas (URIs de árvore SAF) em busca de arquivos de livro (PDF, EPUB, TXT),
 * incluindo subpastas, e resolve metadados de arquivos avulsos adicionados manualmente.
 */
object LibraryScanner {

    data class ScannedFile(val uri: Uri, val name: String, val format: BookFormat)

    /** Varre uma árvore de pastas inteira (recursivo) e retorna todos os arquivos suportados. */
    fun scanTree(context: Context, treeUri: Uri): List<ScannedFile> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val result = mutableListOf<ScannedFile>()
        val pending = ArrayDeque<DocumentFile>()
        pending.addLast(root)

        while (pending.isNotEmpty()) {
            val dir = pending.removeLast()
            val children = try {
                dir.listFiles()
            } catch (e: Exception) {
                emptyArray<DocumentFile>()
            }
            for (child in children) {
                val name = child.name ?: continue
                if (name.startsWith(".")) continue // ignora arquivos/pastas ocultos

                if (child.isDirectory) {
                    pending.addLast(child)
                } else if (child.isFile) {
                    val format = extensionToFormat(name.substringAfterLast('.', ""))
                    if (format != null) {
                        result.add(ScannedFile(child.uri, name, format))
                    }
                }
            }
        }
        return result
    }

    /** Resolve nome e formato de um único arquivo escolhido manualmente pelo usuário. */
    fun resolveSingleFile(context: Context, uri: Uri): ScannedFile? {
        val doc = DocumentFile.fromSingleUri(context, uri) ?: return null
        val name = doc.name ?: return null
        val format = extensionToFormat(name.substringAfterLast('.', "")) ?: return null
        return ScannedFile(uri, name, format)
    }

    fun extensionToFormat(extension: String): BookFormat? = when (extension.lowercase()) {
        "pdf" -> BookFormat.PDF
        "epub" -> BookFormat.EPUB
        "txt" -> BookFormat.TXT
        else -> null
    }

    /** Tipos MIME usados no seletor de "adicionar arquivo avulso". */
    val SUPPORTED_MIME_TYPES = arrayOf(
        "application/pdf",
        "application/epub+zip",
        "text/plain"
    )
}
