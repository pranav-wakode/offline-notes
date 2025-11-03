package com.example.offlinenotes.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val color: Int = -1, // Optional: For color-coding
    val isPinned: Boolean = false // Optional: For pinning
) : Serializable // Serializable to pass between fragments
3. Data Layer (Room Database)This layer handles all data persistence.a. DAO (Data Access Object)This interface defines all the database operations (CRUD).