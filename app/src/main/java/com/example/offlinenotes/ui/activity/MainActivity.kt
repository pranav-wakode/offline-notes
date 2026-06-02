package com.example.offlinenotes.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.offlinenotes.R
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.model.Note
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.noteEditorFragment || destination.id == R.id.scheduleEditorFragment) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
            }
        }

        setupActionBarWithNavController(navController)

        // Handle incoming intents (like clicking a notification)
        handleIntent(intent, navHostFragment)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        intent?.let { handleIntent(it, navHostFragment) }
    }

    private fun handleIntent(intent: Intent, navHostFragment: NavHostFragment) {
        val noteId = intent.getIntExtra("noteId", 0)
        if (noteId > 0) {
            // Fetch the specific note from the database to open it
            val database = NoteDatabase.getDatabase(this)
            val liveData = database.getNoteDao().getNoteById(noteId)

            // FIXED: Use explicit non-nullable Note type and extract to variable for clean removal
            val observer = object : Observer<Note> {
                override fun onChanged(value: Note) {
                    val bundle = Bundle().apply { putSerializable("note", value) }
                    navHostFragment.navController.navigate(R.id.noteEditorFragment, bundle)

                    // Remove observer immediately so it only triggers once on click
                    liveData.removeObserver(this)
                }
            }
            liveData.observe(this, observer)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}