package com.example.offlinenotes.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "schedules_table")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val gridData: String, // Serialized JSON representation of Columns, Rows, and Cells
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) : Serializable