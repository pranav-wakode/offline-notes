package com.example.offlinenotes.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
// Import the switchMap extension function
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.offlinenotes.data.repository.NoteRepository
import com.example.offlinenotes.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteViewModel(application: Application, private val repository: NoteRepository) : AndroidViewModel(application) {

    val allNotes: LiveData<List<Note>> = repository.allNotes
    private val searchQuery = MutableLiveData<String>("")

    // Use the modern 'switchMap' extension function directly on 'searchQuery'
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

    fun update(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(note)
    }
}