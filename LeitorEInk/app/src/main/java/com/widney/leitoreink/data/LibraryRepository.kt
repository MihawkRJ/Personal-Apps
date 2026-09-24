/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.widney.leitoreink.scan.LibraryScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ponto único que junta o scanner de arquivos (SAF) com o banco Room.
 * Todo acesso a disco roda em Dispatchers.IO.
 */
class LibraryRepository(private val context: Context, private val db: AppDatabase) {

    fun observeBooks() = db.bookDao().observeAll()
    fun observeFolders() = db.folderDao().observeAll()

    suspend fun addFolder(treeUri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val name = DocumentFile.fromTreeUri(context, treeUri)?.name ?: "Pasta"
        val folder = ScannedFolder(uriString = treeUri.toString(), displayName = name)
        db.folderDao().insert(folder)
        rescanFolder(folder)
    }

    suspend fun removeFolder(folder: ScannedFolder) = withContext(Dispatchers.IO) {
        db.bookDao().deleteAllFromFolder(folder.uriString)
        db.folderDao().delete(folder)
    }

    suspend fun addSingleFile(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val scanned = LibraryScanner.resolveSingleFile(context, uri) ?: return@withContext
        val book = Book(
            uriString = scanned.uri.toString(),
            displayName = scanned.name,
            format = scanned.format,
            sourceFolderUri = null
        )
        db.bookDao().insert(book)
    }

    /** Reescaneia todas as pastas configuradas: adiciona livros novos e remove os que sumiram. */
    suspend fun rescanAll() = withContext(Dispatchers.IO) {
        val folders = db.folderDao().getAll()
        for (folder in folders) {
            rescanFolder(folder)
        }
    }

    private suspend fun rescanFolder(folder: ScannedFolder) = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(folder.uriString)
        val found = LibraryScanner.scanTree(context, treeUri)
        val foundUris = found.map { it.uri.toString() }.toSet()

        // Usa um loop comum (em vez de .filter { } com chamada suspend dentro do lambda,
        // o que não compilaria) para checar cada arquivo encontrado contra o banco.
        val newBooks = mutableListOf<Book>()
        for (scanned in found) {
            val alreadyKnown = db.bookDao().getByUri(scanned.uri.toString()) != null
            if (!alreadyKnown) {
                newBooks.add(
                    Book(
                        uriString = scanned.uri.toString(),
                        displayName = scanned.name,
                        format = scanned.format,
                        sourceFolderUri = folder.uriString
                    )
                )
            }
        }
        if (newBooks.isNotEmpty()) {
            db.bookDao().insertAll(newBooks)
        }

        // Remove do app os livros desta pasta que não existem mais no disco.
        val knownUris = db.bookDao().getUrisFromFolder(folder.uriString)
        val goneUris = knownUris.filterNot { it in foundUris }
        if (goneUris.isNotEmpty()) {
            db.bookDao().deleteByUris(goneUris)
        }
    }

    suspend fun saveProgress(bookId: Long, position: Int) = withContext(Dispatchers.IO) {
        db.bookDao().saveProgress(bookId, position, System.currentTimeMillis())
    }

    suspend fun getBook(bookId: Long): Book? = withContext(Dispatchers.IO) {
        db.bookDao().getById(bookId)
    }

    suspend fun removeBook(book: Book) = withContext(Dispatchers.IO) {
        db.bookDao().delete(book)
    }
}
