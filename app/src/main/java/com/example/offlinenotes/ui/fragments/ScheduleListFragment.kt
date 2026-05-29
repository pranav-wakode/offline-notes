package com.example.offlinenotes.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.offlinenotes.R
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.ui.adapter.ScheduleAdapter
import com.example.offlinenotes.ui.viewmodel.NoteViewModel
import com.example.offlinenotes.ui.viewmodel.NoteViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ScheduleListFragment : Fragment() {

    private lateinit var viewModel: NoteViewModel
    private lateinit var scheduleAdapter: ScheduleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_schedule_list, container, false)

        val database = NoteDatabase.getDatabase(requireContext())
        val factory = NoteViewModelFactory(requireActivity().application, database)
        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_schedules)
        scheduleAdapter = ScheduleAdapter { schedule ->
            val action = ScheduleListFragmentDirections.actionScheduleListFragmentToScheduleEditorFragment(schedule)
            findNavController().navigate(action)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = scheduleAdapter
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_schedule).setOnClickListener {
            findNavController().navigate(R.id.action_scheduleListFragment_to_scheduleEditorFragment)
        }

        viewModel.allSchedules.observe(viewLifecycleOwner) { schedules ->
            schedules?.let { scheduleAdapter.submitList(it) }
        }

        return view
    }
}