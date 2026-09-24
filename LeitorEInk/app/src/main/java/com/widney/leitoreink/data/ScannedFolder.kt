/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.data

import androidx.room.Entity

/** Uma pasta (árvore SAF) que o usuário escolheu para o app escanear em busca de livros. */
@Entity(tableName = "folders", primaryKeys = ["uriString"])
data class ScannedFolder(
    val uriString: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)
