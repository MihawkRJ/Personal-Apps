/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY lastOpenedAt DESC, displayName ASC")
    fun observeAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Book?

    @Query("SELECT * FROM books WHERE uriString = :uri LIMIT 1")
    suspend fun getByUri(uri: String): Book?

    @Query("SELECT uriString FROM books WHERE sourceFolderUri = :folderUri")
    suspend fun getUrisFromFolder(folderUri: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: Book): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(books: List<Book>)

    @Update
    suspend fun update(book: Book)

    @Query("UPDATE books SET lastPositionIndex = :position, lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun saveProgress(id: Long, position: Int, openedAt: Long)

    @Delete
    suspend fun delete(book: Book)

    @Query("DELETE FROM books WHERE uriString IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    @Query("DELETE FROM books WHERE sourceFolderUri = :folderUri")
    suspend fun deleteAllFromFolder(folderUri: String)
}
