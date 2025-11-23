package com.example.offlinenotes.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.offlinenotes.R
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.data.repository.NoteRepository
import com.example.offlinenotes.model.Note
import com.example.offlinenotes.ui.adapter.NoteAdapter
import com.example.offlinenotes.ui.viewmodel.NoteViewModel
import com.example.offlinenotes.ui.viewmodel.NoteViewModelFactory
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    // ActivityResultLauncher for Exporting (creating a file)
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportData(it) }
    }

    // ActivityResultLauncher for Importing (opening a file)
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importData(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_note_list, container, false)

        val database = NoteDatabase.getDatabase(requireContext())
        val repository = NoteRepository(database.getNoteDao())
        val factory = NoteViewModelFactory(requireActivity().application, repository)
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        recyclerView = view.findViewById(R.id.recycler_view)
        setupRecyclerView()

        fab = view.findViewById(R.id.fab_add_note)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_noteListFragment_to_noteEditorFragment)
        }

        noteViewModel.searchedNotes.observe(viewLifecycleOwner) { notes ->
            notes?.let { noteAdapter.submitList(it) }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter { note ->
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteEditorFragment(note)
            findNavController().navigate(action)
        }
        recyclerView.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = noteAdapter
        }
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

    // --- Logic for Exporting ---
    private fun exportData(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notes = noteViewModel.getAllNotesForExport()
                val gson = Gson()
                val jsonString = gson.toJson(notes)

                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                Toast.makeText(context, "Notes exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    // --- Logic for Importing ---
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

                val jsonString = stringBuilder.toString()
                val gson = Gson()
                val listType = object : TypeToken<List<Note>>() {}.type
                val notes: List<Note> = gson.fromJson(jsonString, listType)

                // Reset IDs to 0 so Room treats them as new entries (avoiding ID conflicts)
                // Or keep them to overwrite. Here we treat them as new imports/overwrites based on logic.
                // Since we used OnConflictStrategy.REPLACE, existing IDs will be updated.
                // If you want to duplicate, set id = 0. Let's respect the backup (REPLACE).

                noteViewModel.importNotes(notes)
                Toast.makeText(context, "${notes.size} notes imported!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed. Invalid JSON?", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}