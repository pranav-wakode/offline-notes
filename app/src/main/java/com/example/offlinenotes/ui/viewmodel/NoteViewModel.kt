package com.example.offlinenotes.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.offlinenotes.data.repository.NoteRepository
import com.example.offlinenotes.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteViewModel(application: Application, private val repository: NoteRepository) : AndroidViewModel(application) {

    val allNotes: LiveData<List<Note>> = repository.allNotes
    private val searchQuery = MutableLiveData<String>("")

    val searchedNotes: LiveData<List<Note>> = searchQuery.switchMap { query ->
        if (query.isNullOrEmpty()) {
            repository.allNotes
        } else {
            repository.searchNotes(query)
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun insert(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(note)
    }

    fun importNotes(notes: List<Note>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertAll(notes)
    }

    // Returns a list directly for export
    suspend fun getAllNotesForExport(): List<Note> = withContext(Dispatchers.IO) {
        repository.getAllNotesSync()
    }

    fun update(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(note)
    }
}