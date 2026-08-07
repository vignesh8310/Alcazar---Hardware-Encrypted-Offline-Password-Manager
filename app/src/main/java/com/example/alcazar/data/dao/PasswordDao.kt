package com.example.alcazar.data.dao

import androidx.room.*
import com.example.alcazar.data.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords WHERE isDuress = :fetchDuress")
    fun getAllPasswords(fetchDuress: Boolean): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM passwords WHERE isDuress = :fetchDuress")
    fun getAllPasswordsSync(fetchDuress: Boolean): List<PasswordEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(entry: PasswordEntry)

    @Update
    suspend fun updatePassword(entry: PasswordEntry)

    @Delete
    suspend fun deletePassword(entry: PasswordEntry)
}