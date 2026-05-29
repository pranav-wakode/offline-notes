package com.example.offlinenotes.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.offlinenotes.data.repository.NoteRepository
import com.example.offlinenotes.model.Folder
import com.example.offlinenotes.model.Note
import com.example.offlinenotes.model.Schedule
import com.example.offlinenotes.utils.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteViewModel(application: Application, private val repository: NoteRepository) : AndroidViewModel(application) {

    val vaultManager = VaultManager(application)

    val allNotes: LiveData<List<Note>> = repository.allNotes
    val rootNotes: LiveData<List<Note>> = repository.rootNotes
    val allFolders: LiveData<List<Folder>> = repository.allFolders
    val allSchedules: LiveData<List<Schedule>> = repository.allSchedules

    private val searchQuery = MutableLiveData<String>("")
    private val currentFolderId = MutableLiveData<Int?>(null)

    val displayNotes: LiveData<List<Note>> = searchQuery.switchMap { query ->
        currentFolderId.switchMap { folderId ->
            if (query.isNullOrEmpty()) {
                if (folderId == null) repository.rootNotes else repository.getNotesByFolder(folderId)
            } else {
                repository.searchNotes(query)
            }
        }
    }

    // --- Vault Decryption Stream ---
    // Safely decrypts notes for display. Uses a single payload to prevent IV reuse.
    val decryptedVaultNotes: LiveData<List<Note>> = repository.vaultNotes.map { encryptedList ->
        if (!vaultManager.isVaultUnlocked) return@map emptyList()

        encryptedList.mapNotNull { encryptedNote ->
            try {
                if (encryptedNote.iv == null) return@mapNotNull encryptedNote

                // Decrypt the combined payload
                val decryptedText = vaultManager.decryptNoteData(encryptedNote.content, encryptedNote.iv)

                // Split back into Title and Content
                val parts = decryptedText.split("|||---|||", limit = 2)
                val dTitle = parts.getOrNull(0) ?: ""
                val dContent = parts.getOrNull(1) ?: ""

                // Return mapped Note for the UI
                encryptedNote.copy(title = dTitle, content = dContent)
            } catch (e: Exception) {
                encryptedNote.copy(title = "🔒 Decryption Failed", content = "Data may be corrupted or password changed.")
            }
        }
    }

    fun setSearchQuery(query: String) { searchQuery.value = query }
    fun setCurrentFolder(folderId: Int?) { currentFolderId.value = folderId }

    // --- Notes ---
    fun insert(note: Note) = viewModelScope.launch(Dispatchers.IO) { repository.insert(note) }
    fun update(note: Note) = viewModelScope.launch(Dispatchers.IO) { repository.update(note) }
    fun delete(note: Note) = viewModelScope.launch(Dispatchers.IO) { repository.delete(note) }
    fun importNotes(notes: List<Note>) = viewModelScope.launch(Dispatchers.IO) { repository.insertAll(notes) }
    suspend fun getAllNotesForExport(): List<Note> = withContext(Dispatchers.IO) { repository.getAllNotesSync() }
    suspend fun getVaultNotesForExport(): List<Note> = withContext(Dispatchers.IO) { repository.getVaultNotesSync() }

    // --- Folders & Schedules ---
    fun insertFolder(folder: Folder) = viewModelScope.launch(Dispatchers.IO) { repository.insertFolder(folder) }
    fun updateFolder(folder: Folder) = viewModelScope.launch(Dispatchers.IO) { repository.updateFolder(folder) }
    fun deleteFolder(folder: Folder) = viewModelScope.launch(Dispatchers.IO) { repository.deleteFolder(folder) }
    fun importFolders(folders: List<Folder>) = viewModelScope.launch(Dispatchers.IO) { repository.insertAllFolders(folders) }
    suspend fun getAllFoldersForExport(): List<Folder> = withContext(Dispatchers.IO) { repository.getAllFoldersSync() }

    fun insertSchedule(schedule: Schedule) = viewModelScope.launch(Dispatchers.IO) { repository.insertSchedule(schedule) }
    fun updateSchedule(schedule: Schedule) = viewModelScope.launch(Dispatchers.IO) { repository.updateSchedule(schedule) }
    fun deleteSchedule(schedule: Schedule) = viewModelScope.launch(Dispatchers.IO) { repository.deleteSchedule(schedule) }
    fun importSchedules(schedules: List<Schedule>) = viewModelScope.launch(Dispatchers.IO) { repository.insertAllSchedules(schedules) }
    suspend fun getAllSchedulesForExport(): List<Schedule> = withContext(Dispatchers.IO) { repository.getAllSchedulesSync() }
}