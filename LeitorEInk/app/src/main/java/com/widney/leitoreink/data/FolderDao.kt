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
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY displayName ASC")
    fun observeAll(): Flow<List<ScannedFolder>>

    @Query("SELECT * FROM folders ORDER BY displayName ASC")
    suspend fun getAll(): List<ScannedFolder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: ScannedFolder)

    @Delete
    suspend fun delete(folder: ScannedFolder)
}
