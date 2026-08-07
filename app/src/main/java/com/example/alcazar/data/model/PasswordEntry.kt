package com.example.alcazar.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val username: String,
    val encryptedPassword: String,
    val category: String = "All",
    val isDuress: Boolean = false // 🔥 NEW: Marks this entry as a decoy honey-pot
)