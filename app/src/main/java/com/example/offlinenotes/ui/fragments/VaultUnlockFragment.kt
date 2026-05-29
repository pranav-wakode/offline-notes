package com.example.offlinenotes.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.offlinenotes.R
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.ui.viewmodel.NoteViewModel
import com.example.offlinenotes.ui.viewmodel.NoteViewModelFactory
import com.google.android.material.textfield.TextInputEditText

class VaultUnlockFragment : Fragment() {

    private lateinit var viewModel: NoteViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_vault_unlock, container, false)

        val database = NoteDatabase.getDatabase(requireContext())
        val factory = NoteViewModelFactory(requireActivity().application, database)
        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        val etPass = view.findViewById<TextInputEditText>(R.id.et_unlock_password)
        val btnUnlock = view.findViewById<Button>(R.id.btn_unlock_vault)

        btnUnlock.setOnClickListener {
            val pass = etPass.text.toString()

            if (viewModel.vaultManager.unlockVault(pass)) {
                findNavController().navigate(R.id.action_vaultUnlockFragment_to_vaultListFragment)
            } else {
                Toast.makeText(context, "Incorrect Password", Toast.LENGTH_SHORT).show()
                etPass.text?.clear()
            }
        }

        return view
    }
}