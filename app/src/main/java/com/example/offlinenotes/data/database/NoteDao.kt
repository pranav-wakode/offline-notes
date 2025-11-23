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

    @Query("SELECT * FROM notes_table ORDER BY isPinned DESC, modifiedAt DESC")
    fun getAllNotes(): LiveData<List<Note>>

    // One-shot query for Export
    @Query("SELECT * FROM notes_table")
    suspend fun getAllNotesSync(): List<Note>

    @Query("SELECT * FROM notes_table WHERE title LIKE :query OR content LIKE :query ORDER BY isPinned DESC, modifiedAt DESC")
    fun searchNotes(query: String): LiveData<List<Note>>

    @Query("SELECT * FROM notes_table WHERE id = :noteId")
    fun getNoteById(noteId: Int): LiveData<Note>
}