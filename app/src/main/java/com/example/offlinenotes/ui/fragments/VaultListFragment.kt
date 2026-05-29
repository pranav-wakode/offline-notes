package com.example.offlinenotes.ui.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.offlinenotes.R
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.model.Note
import com.example.offlinenotes.ui.adapter.NoteAdapter
import com.example.offlinenotes.ui.viewmodel.NoteViewModel
import com.example.offlinenotes.ui.viewmodel.NoteViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class VaultListFragment : Fragment(), MenuProvider {

    private lateinit var viewModel: NoteViewModel
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_vault_list, container, false)
        val database = NoteDatabase.getDatabase(requireContext())
        val factory = NoteViewModelFactory(requireActivity().application, database)
        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        val rvVault = view.findViewById<RecyclerView>(R.id.recycler_view_vault)
        noteAdapter = NoteAdapter { note ->
            val action = VaultListFragmentDirections.actionVaultListFragmentToNoteEditorFragment(note)
            findNavController().navigate(action)
        }
        rvVault.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = noteAdapter
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_vault_note).setOnClickListener {
            // Create an empty note explicitly flagged as Vault to pass to editor
            val newSecureNote = Note(title = "", content = "", isVault = true)
            val action = VaultListFragmentDirections.actionVaultListFragmentToNoteEditorFragment(newSecureNote)
            findNavController().navigate(action)
        }

        viewModel.decryptedVaultNotes.observe(viewLifecycleOwner) { notes ->
            notes?.let { noteAdapter.submitList(it) }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.vault_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_lock_vault) {
            viewModel.vaultManager.lockVault()
            Toast.makeText(context, "Vault Locked", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return true
        }
        return false
    }
}