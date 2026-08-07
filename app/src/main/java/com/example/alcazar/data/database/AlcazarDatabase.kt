package com.example.alcazar.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.alcazar.data.dao.NoteDao
import com.example.alcazar.data.dao.PasswordDao
import com.example.alcazar.data.model.NoteEntry
import com.example.alcazar.data.model.PasswordEntry

// 1. Change version to 2
@Database(entities = [PasswordEntry::class, NoteEntry::class], version = 2, exportSchema = false)
abstract class AlcazarDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var Instance: AlcazarDatabase? = null

        fun getDatabase(context: Context): AlcazarDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    AlcazarDatabase::class.java,
                    "alcazar_vault_database"
                )
                    // 2. Add this line to prevent crashes when you alter table columns!
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}