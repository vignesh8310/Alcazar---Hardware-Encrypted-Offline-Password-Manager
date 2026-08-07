package com.example.alcazar.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val encryptedContent: String,
    val isDuress: Boolean = false // Enforces separation for notes
)