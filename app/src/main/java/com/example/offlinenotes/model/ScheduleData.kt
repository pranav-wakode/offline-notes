package com.example.offlinenotes.model

import java.io.Serializable

data class ScheduleData(
    val columns: MutableList<String> = mutableListOf("Monday", "Tuesday", "Wednesday"),
    val rows: MutableList<String> = mutableListOf("08:00", "09:00", "10:00"),
    val cells: MutableMap<String, String> = mutableMapOf() // Key format: "rowIndex_colIndex"
) : Serializable