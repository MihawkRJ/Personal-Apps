/*
 * Leitor E-Ink
 * Copyright (c) 2026 Widney Silva
 * Licenciado sob a Licença MIT. Veja o arquivo LICENSE na raiz do repositório.
 */
package com.widney.leitoreink.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Book::class, ScannedFolder::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "leitor_eink.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
