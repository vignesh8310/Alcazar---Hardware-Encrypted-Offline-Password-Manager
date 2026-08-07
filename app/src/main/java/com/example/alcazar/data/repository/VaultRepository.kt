package com.example.alcazar.data.repository

import com.example.alcazar.data.dao.NoteDao
import com.example.alcazar.data.dao.PasswordDao
import com.example.alcazar.data.model.NoteEntry
import com.example.alcazar.data.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

class VaultRepository(
    private val passwordDao: PasswordDao,
    private val noteDao: NoteDao
) {
    // Flows for real‑time data
    fun getAllPasswords(fetchDuress: Boolean): Flow<List<PasswordEntry>> =
        passwordDao.getAllPasswords(fetchDuress)

    fun getAllNotes(fetchDuress: Boolean): Flow<List<NoteEntry>> =
        noteDao.getAllNotes(fetchDuress)

    // Synchronous versions for backup/export and category updates
    fun getAllPasswordsSync(fetchDuress: Boolean): List<PasswordEntry> =
        passwordDao.getAllPasswordsSync(fetchDuress)

    fun getAllNotesSync(fetchDuress: Boolean): List<NoteEntry> =
        noteDao.getAllNotesSync(fetchDuress)

    // CRUD operations
    suspend fun insertPassword(entry: PasswordEntry) = passwordDao.insertPassword(entry)
    suspend fun updatePassword(entry: PasswordEntry) = passwordDao.updatePassword(entry)
    suspend fun deletePassword(entry: PasswordEntry) = passwordDao.deletePassword(entry)

    suspend fun insertNote(entry: NoteEntry) = noteDao.insertNote(entry)
    suspend fun updateNote(entry: NoteEntry) = noteDao.updateNote(entry)
    suspend fun deleteNote(entry: NoteEntry) = noteDao.deleteNote(entry)
}