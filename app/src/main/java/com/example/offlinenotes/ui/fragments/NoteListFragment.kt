package com.example.offlinenotes.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.offlinenotes.R
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.data.repository.NoteRepository
import com.example.offlinenotes.ui.adapter.NoteAdapter
import com.example.offlinenotes.ui.viewmodel.NoteViewModel
import com.example.offlinenotes.ui.viewmodel.NoteViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NoteListFragment : Fragment() {

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_note_list, container, false)

        // Initialize ViewModel
        val database = NoteDatabase.getDatabase(requireContext())
        val repository = NoteRepository(database.getNoteDao())
        val factory = NoteViewModelFactory(requireActivity().application, repository)
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view)
        setupRecyclerView()

        // Setup FAB
        fab = view.findViewById(R.id.fab_add_note)
        fab.setOnClickListener {
            // Navigate to NoteEditorFragment (with no noteId)
            findNavController().navigate(R.id.action_noteListFragment_to_noteEditorFragment)
        }
        
        // Observe LiveData
        noteViewModel.searchedNotes.observe(viewLifecycleOwner) { notes ->
            notes?.let { noteAdapter.submitList(it) }
        }

        setHasOptionsMenu(true) // Enable options menu (for search)
        return view
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter { note ->
            // On note clicked, navigate to editor with the note
            val action = NoteListFragmentDirections.actionNoteListFragmentToNoteEditorFragment(note)
            findNavController().navigate(action)
        }
        recyclerView.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = noteAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.list_menu, menu)
        
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
        
        super.onCreateOptionsMenu(menu, inflater)
    }
}