package com.example.offlinenotes.data.repository

import androidx.lifecycle.LiveData
import com.example.offlinenotes.data.database.FolderDao
import com.example.offlinenotes.data.database.NoteDao
import com.example.offlinenotes.data.database.ScheduleDao
import com.example.offlinenotes.model.Folder
import com.example.offlinenotes.model.Note
import com.example.offlinenotes.model.Schedule

class NoteRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val scheduleDao: ScheduleDao
) {
    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes()
    val rootNotes: LiveData<List<Note>> = noteDao.getRootNotes()
    val vaultNotes: LiveData<List<Note>> = noteDao.getVaultNotes() // Added Vault Stream

    val allFolders: LiveData<List<Folder>> = folderDao.getAllFolders()
    val allSchedules: LiveData<List<Schedule>> = scheduleDao.getAllSchedules()

    // --- Note Ops ---
    fun getNotesByFolder(folderId: Int): LiveData<List<Note>> = noteDao.getNotesByFolder(folderId)
    suspend fun insert(note: Note) = noteDao.insertNote(note)
    suspend fun insertAll(notes: List<Note>) = noteDao.insertAll(notes)
    suspend fun getAllNotesSync(): List<Note> = noteDao.getAllNotesSync()
    suspend fun getVaultNotesSync(): List<Note> = noteDao.getVaultNotesSync()
    suspend fun update(note: Note) = noteDao.updateNote(note)
    suspend fun delete(note: Note) = noteDao.deleteNote(note)
    fun searchNotes(query: String): LiveData<List<Note>> = noteDao.searchNotes("%$query%")
    fun getNoteById(noteId: Int): LiveData<Note> = noteDao.getNoteById(noteId)

    // --- Folder Ops ---
    suspend fun insertFolder(folder: Folder) = folderDao.insertFolder(folder)
    suspend fun updateFolder(folder: Folder) = folderDao.updateFolder(folder)
    suspend fun deleteFolder(folder: Folder) {
        noteDao.moveNotesToRoot(folder.id)
        folderDao.deleteFolder(folder)
    }
    suspend fun getAllFoldersSync(): List<Folder> = folderDao.getAllFoldersSync()
    suspend fun insertAllFolders(folders: List<Folder>) = folderDao.insertAll(folders)

    // --- Schedule Ops ---
    suspend fun insertSchedule(schedule: Schedule) = scheduleDao.insertSchedule(schedule)
    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.updateSchedule(schedule)
    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.deleteSchedule(schedule)
    suspend fun getAllSchedulesSync(): List<Schedule> = scheduleDao.getAllSchedulesSync()
    suspend fun insertAllSchedules(schedules: List<Schedule>) = scheduleDao.insertAll(schedules)
}