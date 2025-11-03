package com.example.offlinenotes.data.repository

import androidx.lifecycle.LiveData
import com.example.offlinenotes.data.database.NoteDao
import com.example.offlinenotes.model.Note

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes()

    suspend fun insert(note: Note) {
        noteDao.insertNote(note)
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
5. ViewModel LayerThis layer holds the UI logic and data, surviving configuration changes.a. ViewModelFactoryThis factory is necessary to pass the Application context (needed for the repository) into the ViewModel.