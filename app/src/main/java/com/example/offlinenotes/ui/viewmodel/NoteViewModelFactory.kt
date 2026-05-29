package com.example.offlinenotes.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.data.repository.NoteRepository

class NoteViewModelFactory(
    private val application: Application,
    private val database: NoteDatabase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            val repository = NoteRepository(
                database.getNoteDao(),
                database.getFolderDao(),
                database.getScheduleDao()
            )
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}