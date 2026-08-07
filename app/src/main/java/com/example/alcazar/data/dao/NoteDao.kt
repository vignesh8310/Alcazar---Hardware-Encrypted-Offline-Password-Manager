package com.example.alcazar.data.dao

import androidx.room.*
import com.example.alcazar.data.model.NoteEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDuress = :fetchDuress")
    fun getAllNotes(fetchDuress: Boolean): Flow<List<NoteEntry>>

    @Query("SELECT * FROM notes WHERE isDuress = :fetchDuress")
    fun getAllNotesSync(fetchDuress: Boolean): List<NoteEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(entry: NoteEntry)

    @Update
    suspend fun updateNote(entry: NoteEntry)

    @Delete
    suspend fun deleteNote(entry: NoteEntry)
}