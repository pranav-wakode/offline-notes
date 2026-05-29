package com.example.offlinenotes.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.offlinenotes.model.Schedule

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<Schedule>)

    @Update
    suspend fun updateSchedule(schedule: Schedule)

    @Delete
    suspend fun deleteSchedule(schedule: Schedule)

    @Query("SELECT * FROM schedules_table ORDER BY modifiedAt DESC")
    fun getAllSchedules(): LiveData<List<Schedule>>

    @Query("SELECT * FROM schedules_table")
    suspend fun getAllSchedulesSync(): List<Schedule>
}