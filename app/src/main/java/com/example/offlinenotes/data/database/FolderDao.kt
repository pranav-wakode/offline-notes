package com.example.offlinenotes.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.offlinenotes.model.Folder

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<Folder>)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("SELECT * FROM folders_table ORDER BY name ASC")
    fun getAllFolders(): LiveData<List<Folder>>

    @Query("SELECT * FROM folders_table")
    suspend fun getAllFoldersSync(): List<Folder>
}