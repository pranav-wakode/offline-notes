package com.example.offlinenotes.data.repository

import androidx.lifecycle.LiveData
import com.example.offlinenotes.data.database.NoteDao
import com.example.offlinenotes.model.Note

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes()

    suspend fun insert(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun insertAll(notes: List<Note>) {
        noteDao.insertAll(notes)
    }

    suspend fun getAllNotesSync(): List<Note> {
        return noteDao.getAllNotesSync()
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun delete(note: Note) {
        noteDao.deleteNote(note)
    }

    fun searchNotes(query: String): LiveData<List<Note>> {
        return noteDao.searchNotes("%$query%")
    }

    fun getNoteById(noteId: Int): LiveData<Note> {
        return noteDao.getNoteById(noteId)
    }
}