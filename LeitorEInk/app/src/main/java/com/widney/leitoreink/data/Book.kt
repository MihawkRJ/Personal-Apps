/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BookFormat {
    PDF, EPUB, TXT
}

/**
 * Representa um livro conhecido pelo app. O arquivo em si NUNCA é copiado:
 * guardamos apenas a URI (content:// ou file://) e pedimos permissão
 * persistente de leitura para ela.
 */
@Entity(
    tableName = "books",
    indices = [Index(value = ["uriString"], unique = true)]
)
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val uriString: String,

    val displayName: String,

    val format: BookFormat,

    /** URI da pasta (SAF) de onde este livro veio, ou null se foi adicionado manualmente. */
    val sourceFolderUri: String? = null,

    /**
     * Posição de leitura salva:
     *  - PDF: índice da página (0-based)
     *  - EPUB: índice do capítulo (spine) (0-based)
     *  - TXT: índice da "página" calculada (0-based)
     */
    val lastPositionIndex: Int = 0,

    val addedAt: Long = System.currentTimeMillis(),

    val lastOpenedAt: Long = 0
)
