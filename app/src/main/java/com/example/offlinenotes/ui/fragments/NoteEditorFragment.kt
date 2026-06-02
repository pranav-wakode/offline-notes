package com.example.offlinenotes.ui.fragments

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.offlinenotes.R
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.model.Folder
import com.example.offlinenotes.model.Note
import com.example.offlinenotes.receiver.ReminderReceiver
import com.example.offlinenotes.ui.viewmodel.NoteViewModel
import com.example.offlinenotes.ui.viewmodel.NoteViewModelFactory
import com.example.offlinenotes.utils.BulletManager
import com.example.offlinenotes.utils.BulletType
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class NoteEditorFragment : Fragment(), MenuProvider {

    private lateinit var viewModel: NoteViewModel
    private val args: NoteEditorFragmentArgs by navArgs()
    private var currentNote: Note? = null

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var spinnerFolder: Spinner
    private lateinit var btnVaultToggle: ImageButton

    private var folderList: List<Folder> = emptyList()
    private lateinit var bulletManager: BulletManager
    private var isVaultNote = false

    // Request notification permission for Android 13+
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkExactAlarmPermission()
        } else {
            Toast.makeText(context, "Notifications disabled. Alarms won't show.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_note_editor, container, false)
        val database = NoteDatabase.getDatabase(requireContext())
        val factory = NoteViewModelFactory(requireActivity().application, database)
        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        etTitle = view.findViewById(R.id.et_note_title)
        etContent = view.findViewById(R.id.et_note_content)
        spinnerFolder = view.findViewById(R.id.spinner_folder)
        btnVaultToggle = view.findViewById(R.id.btn_vault_toggle)

        bulletManager = BulletManager(etContent)

        currentNote = args.note

        currentNote?.let {
            isVaultNote = it.isVault
            etTitle.setText(it.title)
            etContent.setText(it.content)
        }

        updateVaultToggleUI()
        setupToolbar(view)

        btnVaultToggle.setOnClickListener {
            if (!isVaultNote && !viewModel.vaultManager.isVaultUnlocked) {
                Toast.makeText(context, "Unlock vault first to secure notes.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isVaultNote = !isVaultNote
            updateVaultToggleUI()
        }

        viewModel.allFolders.observe(viewLifecycleOwner) { folders ->
            folderList = folders
            setupFolderSpinner()
        }

        view.findViewById<FloatingActionButton>(R.id.fab_save).setOnClickListener { saveNote() }

        return view
    }

    private fun setupToolbar(view: View) {
        val btnStandard = view.findViewById<ImageButton>(R.id.btn_bullet_standard)
        val btnCheckbox = view.findViewById<ImageButton>(R.id.btn_bullet_checkbox)
        val btnDate = view.findViewById<ImageButton>(R.id.btn_bullet_date)
        val btnReminder = view.findViewById<ImageButton>(R.id.btn_bullet_reminder)

        btnStandard.setOnClickListener {
            val newType = if (bulletManager.currentBulletType == BulletType.STANDARD) BulletType.NONE else BulletType.STANDARD
            bulletManager.insertBulletAtCursor(newType)
        }

        btnCheckbox.setOnClickListener {
            val newType = if (bulletManager.currentBulletType == BulletType.CHECKBOX) BulletType.NONE else BulletType.CHECKBOX
            bulletManager.insertBulletAtCursor(newType)
        }

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.getDefault(), "📅 %04d-%02d-%02d", year, month + 1, dayOfMonth)
                bulletManager.insertDateOrReminder(formatted)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnReminder.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    checkExactAlarmPermission()
                } else {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                checkExactAlarmPermission()
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                Toast.makeText(requireContext(), "Please grant permission to schedule reminders", Toast.LENGTH_LONG).show()
                return
            }
        }
        showReminderPicker()
    }

    private fun showReminderPicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                val scheduledTime = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, hourOfDay, minute, 0)
                }

                if (scheduledTime.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(context, "Cannot schedule in the past", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                val reminderId = Random.nextInt(10000, 99999)

                // FIXED: Simplified, human-readable bullet format
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                val formatted = "⏰ ${dateFormat.format(scheduledTime.time)}"

                bulletManager.insertDateOrReminder(formatted)
                scheduleOfflineAlarm(scheduledTime.timeInMillis, reminderId)

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun scheduleOfflineAlarm(timeInMillis: Long, reminderId: Int) {
        val titleText = etTitle.text.toString().takeIf { it.isNotBlank() } ?: "Scheduled Note"

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
            putExtra("title", titleText)
            putExtra("noteId", reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            Toast.makeText(context, "Reminder set!", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Failed to schedule alarm: Permission denied", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateVaultToggleUI() {
        if (isVaultNote) {
            btnVaultToggle.setColorFilter(Color.parseColor("#4CAF50"))
        } else {
            btnVaultToggle.setColorFilter(Color.parseColor("#757575"))
        }
    }

    private fun setupFolderSpinner() {
        val folderNames = mutableListOf("No Folder")
        folderNames.addAll(folderList.map { it.name })
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, folderNames)
        spinnerFolder.adapter = adapter

        if (currentNote?.folderId != null) {
            val idx = folderList.indexOfFirst { it.id == currentNote!!.folderId }
            if (idx != -1) spinnerFolder.setSelection(idx + 1)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun saveNote() {
        val rawTitle = etTitle.text.toString().trim()
        val rawContent = etContent.text.toString().trim()

        if (rawTitle.isEmpty() && rawContent.isEmpty()) {
            Toast.makeText(context, "Cannot save empty note", Toast.LENGTH_SHORT).show()
            return
        }

        var finalTitle = rawTitle
        var finalContent = rawContent
        var finalIv: String? = currentNote?.iv

        if (isVaultNote) {
            if (!viewModel.vaultManager.isVaultUnlocked) {
                Toast.makeText(context, "Cannot save secure note while vault is locked.", Toast.LENGTH_LONG).show()
                return
            }
            val payload = "$rawTitle|||---|||$rawContent"
            val (ciphertext, iv) = viewModel.vaultManager.encryptNoteData(payload)

            finalTitle = "🔒 Encrypted Note"
            finalContent = ciphertext
            finalIv = iv
        }

        val selectedPos = spinnerFolder.selectedItemPosition
        val folderId = if (selectedPos > 0) folderList[selectedPos - 1].id else null

        val isNewNote = currentNote == null || currentNote?.id == 0

        val updatedNote = currentNote?.copy(
            title = finalTitle,
            content = finalContent,
            modifiedAt = System.currentTimeMillis(),
            folderId = folderId,
            isVault = isVaultNote,
            iv = finalIv
        ) ?: Note(
            title = finalTitle,
            content = finalContent,
            folderId = folderId,
            isVault = isVaultNote,
            iv = finalIv
        )

        if (isNewNote) {
            viewModel.insert(updatedNote)
        } else {
            viewModel.update(updatedNote)
        }

        Toast.makeText(context, "Note Saved", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (currentNote != null && currentNote?.id != 0) {
            menuInflater.inflate(R.menu.editor_menu, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_delete) {
            currentNote?.let {
                viewModel.delete(it)
                findNavController().navigateUp()
            }
            return true
        }
        return false
    }
}