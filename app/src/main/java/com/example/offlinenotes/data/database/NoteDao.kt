package com.example.offlinenotes.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.offlinenotes.model.Note

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    // Standard queries MUST EXCLUDE isVault = 1 (Vault = 0 or false in SQLite integer terms)
    @Query("SELECT * FROM notes_table WHERE isVault = 0 ORDER BY isPinned DESC, modifiedAt DESC")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes_table WHERE isVault = 0")
    suspend fun getAllNotesSync(): List<Note>

    @Query("SELECT * FROM notes_table WHERE folderId IS NULL AND isVault = 0 ORDER BY isPinned DESC, modifiedAt DESC")
    fun getRootNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes_table WHERE folderId = :folderId AND isVault = 0 ORDER BY isPinned DESC, modifiedAt DESC")
    fun getNotesByFolder(folderId: Int): LiveData<List<Note>>

    @Query("SELECT * FROM notes_table WHERE (title LIKE :query OR content LIKE :query) AND isVault = 0 ORDER BY isPinned DESC, modifiedAt DESC")
    fun searchNotes(query: String): LiveData<List<Note>>

    // --- VAULT QUERIES ---
    @Query("SELECT * FROM notes_table WHERE isVault = 1 ORDER BY modifiedAt DESC")
    fun getVaultNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes_table WHERE isVault = 1")
    suspend fun getVaultNotesSync(): List<Note>

    @Query("UPDATE notes_table SET folderId = NULL WHERE folderId = :folderId")
    suspend fun moveNotesToRoot(folderId: Int)

    @Query("SELECT * FROM notes_table WHERE id = :noteId LIMIT 1")
    fun getNoteById(noteId: Int): LiveData<Note>
}