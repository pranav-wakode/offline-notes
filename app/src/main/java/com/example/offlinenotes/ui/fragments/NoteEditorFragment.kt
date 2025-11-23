package com.example.offlinenotes.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.offlinenotes.R
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.data.repository.NoteRepository
import com.example.offlinenotes.model.Note
import com.example.offlinenotes.ui.viewmodel.NoteViewModel
import com.example.offlinenotes.ui.viewmodel.NoteViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NoteEditorFragment : Fragment(), MenuProvider {

    private lateinit var noteViewModel: NoteViewModel
    private val args: NoteEditorFragmentArgs by navArgs()

    private var currentNote: Note? = null
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var fabSave: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_note_editor, container, false)

        val database = NoteDatabase.getDatabase(requireContext())
        val repository = NoteRepository(database.getNoteDao())
        val factory = NoteViewModelFactory(requireActivity().application, repository)
        noteViewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        etTitle = view.findViewById(R.id.et_note_title)
        etContent = view.findViewById(R.id.et_note_content)
        fabSave = view.findViewById(R.id.fab_save)

        currentNote = args.note

        currentNote?.let {
            etTitle.setText(it.title)
            etContent.setText(it.content)
        }

        fabSave.setOnClickListener {
            saveNote()
        }

        setupAutoBullets()

        return view
    }

    private fun setupAutoBullets() {
        etContent.addTextChangedListener(object : TextWatcher {
            private var previousText = ""
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No operation
            }

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return

                val text = s.toString()
                // Detect if a newline was just added
                if (text.length > previousText.length && text.endsWith("\n") && !previousText.endsWith("\n")) {
                    isFormatting = true

                    try {
                        // Find the start and end of the line *before* the newline
                        // The cursor is currently at the end (after \n)
                        val cursorPosition = etContent.selectionStart
                        val textBeforeCursor = text.substring(0, cursorPosition - 1) // Exclude the new \n
                        val lastLineIndex = textBeforeCursor.lastIndexOf('\n') + 1

                        // Get the content of the line just finished
                        val lastLine = textBeforeCursor.substring(lastLineIndex)

                        // If the line is not empty and doesn't already have a bullet
                        if (lastLine.isNotBlank() && !lastLine.trimStart().startsWith("•")) {
                            // Insert bullet at the start of that line
                            s.insert(lastLineIndex, "• ")
                        }

                        // Optional: If you also want the NEW line to auto-start with a bullet
                        // s.append("• ")

                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isFormatting = false
                    }
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun saveNote() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(context, "Cannot save an empty note", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedNote = currentNote?.copy(
            title = title,
            content = content,
            modifiedAt = System.currentTimeMillis()
        ) ?: Note(
            title = title,
            content = content
        )

        if (currentNote == null) {
            noteViewModel.insert(updatedNote)
            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
        } else {
            noteViewModel.update(updatedNote)
            Toast.makeText(context, "Note updated", Toast.LENGTH_SHORT).show()
        }

        findNavController().navigateUp()
    }

    private fun deleteNote() {
        currentNote?.let {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete") { _, _ ->
                    noteViewModel.delete(it)
                    Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (currentNote != null) {
            menuInflater.inflate(R.menu.editor_menu, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_delete -> {
                deleteNote()
                true
            }
            else -> false
        }
    }
}