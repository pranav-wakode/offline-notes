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

class VaultSetupFragment : Fragment() {

    private lateinit var viewModel: NoteViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_vault_setup, container, false)

        val database = NoteDatabase.getDatabase(requireContext())
        val factory = NoteViewModelFactory(requireActivity().application, database)
        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        val etPass = view.findViewById<TextInputEditText>(R.id.et_password)
        val etConfirm = view.findViewById<TextInputEditText>(R.id.et_confirm_password)
        val btnSetup = view.findViewById<Button>(R.id.btn_setup_vault)

        btnSetup.setOnClickListener {
            val pass = etPass.text.toString()
            val confirm = etConfirm.text.toString()

            if (pass.length < 6) {
                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirm) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Derive key and store verification tokens securely
            viewModel.vaultManager.setupVault(pass)
            Toast.makeText(context, "Vault Secured", Toast.LENGTH_SHORT).show()

            // Navigate to the Vault List
            findNavController().navigate(R.id.action_vaultSetupFragment_to_vaultListFragment)
        }

        return view
    }
}