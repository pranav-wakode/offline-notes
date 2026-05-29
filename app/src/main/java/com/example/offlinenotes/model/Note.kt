package com.example.offlinenotes.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val color: Int = -1,
    val isPinned: Boolean = false,
    val folderId: Int? = null,
    val isVault: Boolean = false, // V4 Security: True if note is encrypted
    val iv: String? = null        // V4 Security: Base64 AES-GCM Initialization Vector
) : Serializable