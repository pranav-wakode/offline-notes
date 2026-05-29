package com.example.offlinenotes.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.offlinenotes.R
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.model.BackupData
import com.example.offlinenotes.model.Folder
import com.example.offlinenotes.model.Note
import com.example.offlinenotes.ui.adapter.FolderAdapter
import com.example.offlinenotes.ui.adapter.NoteAdapter
import com.example.offlinenotes.ui.viewmodel.NoteViewModel
import com.example.offlinenotes.ui.viewmodel.NoteViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Date

class NoteListFragment : Fragment(), MenuProvider {

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var folderAdapter: FolderAdapter

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportData(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importData(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_note_list, container, false)
        val database = NoteDatabase.getDatabase(requireContext())
        val factory = NoteViewModelFactory(requireActivity().application, database)
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        setupRecyclerViews(view)

        // FAB INTERACTION: Pass the current active folder down to the editor via SafeArgs
        view.findViewById<FloatingActionButton>(R.id.fab_add_note).setOnClickListener {
            val activeFolderId = noteViewModel.getCurrentFolderId()
            val newNote = Note(id = 0, title = "", content = "", folderId = activeFolderId)
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteEditorFragment(newNote)
            findNavController().navigate(action)
        }

        noteViewModel.displayNotes.observe(viewLifecycleOwner) { notes ->
            notes?.let { noteAdapter.submitList(it) }
        }
        noteViewModel.allFolders.observe(viewLifecycleOwner) { folders ->
            folders?.let { folderAdapter.submitList(it) }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerViews(view: View) {
        val rvNotes = view.findViewById<RecyclerView>(R.id.recycler_view)
        noteAdapter = NoteAdapter { note ->
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteEditorFragment(note)
            findNavController().navigate(action)
        }
        rvNotes.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = noteAdapter
        }

        val rvFolders = view.findViewById<RecyclerView>(R.id.rv_folders)
        folderAdapter = FolderAdapter(
            onFolderClick = { folder ->
                noteViewModel.setCurrentFolder(folder?.id)
                folderAdapter.setSelectedFolder(folder?.id)
            },
            onFolderLongClick = { folder ->
                showDeleteFolderDialog(folder)
            }
        )
        rvFolders.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = folderAdapter
        }
    }

    private fun showAddFolderDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_folder, null)
        val etFolderName = dialogView.findViewById<EditText>(R.id.et_folder_name)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = etFolderName.text.toString().trim()
                if (name.isNotEmpty()) {
                    noteViewModel.insertFolder(Folder(name = name))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteFolderDialog(folder: Folder) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Folder")
            .setMessage("Are you sure you want to delete '${folder.name}'? Notes inside will be safely moved to 'All Notes'.")
            .setPositiveButton("Delete") { _, _ ->
                noteViewModel.deleteFolder(folder)
                noteViewModel.setCurrentFolder(null)
                folderAdapter.setSelectedFolder(null)
                Toast.makeText(context, "Folder deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.list_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { noteViewModel.setSearchQuery(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { noteViewModel.setSearchQuery(it) }
                return true
            }
        })

        searchView.setOnCloseListener {
            noteViewModel.setSearchQuery("")
            false
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_vault -> {
                if (noteViewModel.vaultManager.isVaultUnlocked) {
                    findNavController().navigate(R.id.action_noteListFragment_to_vaultListFragment)
                } else if (noteViewModel.vaultManager.isVaultSetup) {
                    findNavController().navigate(R.id.action_noteListFragment_to_vaultUnlockFragment)
                } else {
                    findNavController().navigate(R.id.action_noteListFragment_to_vaultSetupFragment)
                }
                true
            }
            R.id.action_add_folder -> {
                showAddFolderDialog()
                true
            }
            R.id.action_export -> {
                val fileName = "notes_backup_${Date().time}.json"
                exportLauncher.launch(fileName)
                true
            }
            R.id.action_import -> {
                importLauncher.launch(arrayOf("application/json"))
                true
            }
            else -> false
        }
    }

    private fun exportData(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notes = noteViewModel.getAllNotesForExport() + noteViewModel.getVaultNotesForExport()
                val folders = noteViewModel.getAllFoldersForExport()
                val schedules = noteViewModel.getAllSchedulesForExport()

                val backupData = BackupData(
                    version = 4,
                    folders = folders,
                    notes = notes,
                    schedules = schedules,
                    vaultSalt = noteViewModel.vaultManager.getVaultSalt(),
                    vaultVerificationToken = noteViewModel.vaultManager.getVaultVerificationToken(),
                    vaultVerificationIv = noteViewModel.vaultManager.getVaultVerificationIv()
                )

                val jsonString = Gson().toJson(backupData)

                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                Toast.makeText(context, "Encrypted Backup Exported Successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun importData(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val contentResolver = requireContext().contentResolver
                val stringBuilder = StringBuilder()
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }

                val jsonString = stringBuilder.toString().trim()
                val gson = Gson()

                if (jsonString.startsWith("[")) {
                    val listType = object : TypeToken<List<Note>>() {}.type
                    val notes: List<Note> = gson.fromJson(jsonString, listType)
                    noteViewModel.importNotes(notes)
                    Toast.makeText(context, "Legacy Backup Restored!", Toast.LENGTH_SHORT).show()
                } else if (jsonString.startsWith("{")) {
                    val backupType = object : TypeToken<BackupData>() {}.type
                    val backupData: BackupData = gson.fromJson(jsonString, backupType)

                    noteViewModel.importFolders(backupData.folders)
                    noteViewModel.importNotes(backupData.notes)
                    noteViewModel.importSchedules(backupData.schedules)

                    if (backupData.vaultSalt != null && backupData.vaultVerificationToken != null && backupData.vaultVerificationIv != null) {
                        noteViewModel.vaultManager.restoreVaultAuth(
                            backupData.vaultSalt,
                            backupData.vaultVerificationToken,
                            backupData.vaultVerificationIv
                        )
                        Toast.makeText(context, "Vault State Restored. Vault is now locked.", Toast.LENGTH_LONG).show()
                    }

                    Toast.makeText(context, "Full Backup Restored Successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    throw Exception("Unrecognized backup format.")
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Import failed. Invalid JSON?", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}