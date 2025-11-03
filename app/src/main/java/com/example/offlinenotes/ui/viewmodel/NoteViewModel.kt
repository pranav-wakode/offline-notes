package com.example.offlinenotes.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.offlinenotes.data.repository.NoteRepository
import com.example.offlinenotes.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteViewModel(application: Application, private val repository: NoteRepository) : AndroidViewModel(application) {

    val allNotes: LiveData<List<Note>> = repository.allNotes
    private val searchQuery = MutableLiveData<String>("")

    val searchedNotes: LiveData<List<Note>> = Transformations.switchMap(searchQuery) { query ->
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
6. UI Layer (View)This includes the Activity, Fragments, and RecyclerView Adapter.a. RecyclerView AdapterManages the list/grid of notes.