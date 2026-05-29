package com.example.offlinenotes.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
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
import com.example.offlinenotes.model.Schedule
import com.example.offlinenotes.model.ScheduleData
import com.example.offlinenotes.ui.viewmodel.NoteViewModel
import com.example.offlinenotes.ui.viewmodel.NoteViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson

class ScheduleEditorFragment : Fragment(), MenuProvider {

    private lateinit var viewModel: NoteViewModel
    private val args: ScheduleEditorFragmentArgs by navArgs()

    private var currentSchedule: Schedule? = null
    private lateinit var etTitle: EditText
    private lateinit var tableLayout: TableLayout

    private val gson = Gson()
    private var scheduleData = ScheduleData()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_editor, container, false)

        val database = NoteDatabase.getDatabase(requireContext())
        val factory = NoteViewModelFactory(requireActivity().application, database)
        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        etTitle = view.findViewById(R.id.et_schedule_title)
        tableLayout = view.findViewById(R.id.table_layout_grid)

        currentSchedule = args.schedule
        currentSchedule?.let {
            etTitle.setText(it.name)
            try {
                scheduleData = gson.fromJson(it.gridData, ScheduleData::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        view.findViewById<Button>(R.id.btn_add_col).setOnClickListener {
            scheduleData.columns.add("New Col")
            renderGrid()
        }

        view.findViewById<Button>(R.id.btn_add_row).setOnClickListener {
            scheduleData.rows.add("New Row")
            renderGrid()
        }

        view.findViewById<FloatingActionButton>(R.id.fab_save_schedule).setOnClickListener {
            saveSchedule()
        }

        renderGrid()
        return view
    }

    private fun renderGrid() {
        tableLayout.removeAllViews()

        // 1. Header Row (Top Left corner is empty label, then dynamic Columns)
        val headerRow = TableRow(requireContext())
        headerRow.addView(createLabelCell("Time \\ Day", true))

        scheduleData.columns.forEachIndexed { colIndex, colName ->
            headerRow.addView(createEditableCell(colName, true) { newName ->
                scheduleData.columns[colIndex] = newName
            })
        }
        tableLayout.addView(headerRow)

        // 2. Data Rows
        scheduleData.rows.forEachIndexed { rowIndex, rowName ->
            val tableRow = TableRow(requireContext())

            // First cell in a row is the Row Label (e.g., Time Slot)
            tableRow.addView(createEditableCell(rowName, true) { newName ->
                scheduleData.rows[rowIndex] = newName
            })

            // Data Cells
            scheduleData.columns.forEachIndexed { colIndex, _ ->
                val key = "${rowIndex}_${colIndex}"
                val content = scheduleData.cells[key] ?: ""

                tableRow.addView(createEditableCell(content, false) { newContent ->
                    scheduleData.cells[key] = newContent
                })
            }
            tableLayout.addView(tableRow)
        }
    }

    private fun createLabelCell(text: String, isHeader: Boolean): TextView {
        val tv = TextView(requireContext())
        tv.text = text
        tv.setPadding(16, 16, 16, 16)
        if (isHeader) {
            tv.setBackgroundColor(Color.parseColor("#E0E0E0")) // Light grey for header distinction
            tv.setTextColor(Color.BLACK)
        }
        return tv
    }

    private fun createEditableCell(initialText: String, isHeader: Boolean, onTextChanged: (String) -> Unit): EditText {
        val et = EditText(requireContext())
        et.setText(initialText)
        et.setPadding(24, 24, 24, 24)
        et.minWidth = 250
        et.minLines = 2
        et.gravity = Gravity.TOP or Gravity.START
        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        et.setBackgroundResource(android.R.drawable.edit_text)

        if (isHeader) {
            et.setBackgroundColor(Color.parseColor("#F5F5F5"))
            et.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged(s.toString())
            }
        })
        return et
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun saveSchedule() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        // The TextWatchers update scheduleData dynamically. 
        // We just serialize it.
        val jsonGrid = gson.toJson(scheduleData)

        val updatedSchedule = currentSchedule?.copy(
            name = title,
            gridData = jsonGrid,
            modifiedAt = System.currentTimeMillis()
        ) ?: Schedule(
            name = title,
            gridData = jsonGrid
        )

        if (currentSchedule == null) {
            viewModel.insertSchedule(updatedSchedule)
            Toast.makeText(context, "Schedule created", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.updateSchedule(updatedSchedule)
            Toast.makeText(context, "Schedule updated", Toast.LENGTH_SHORT).show()
        }

        findNavController().navigateUp()
    }

    private fun deleteSchedule() {
        currentSchedule?.let {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Schedule")
                .setMessage("Are you sure you want to delete this schedule?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteSchedule(it)
                    Toast.makeText(context, "Schedule deleted", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (currentSchedule != null) menuInflater.inflate(R.menu.editor_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_delete -> {
                deleteSchedule()
                true
            }
            else -> false
        }
    }
}