package com.example.offlinenotes.ui.fragments

import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavDirections
import com.example.offlinenotes.R
import com.example.offlinenotes.model.Note
import java.io.Serializable
import kotlin.Int
import kotlin.Suppress

public class NoteListFragmentDirections private constructor() {
  private data class ActionNoteListFragmentToNoteEditorFragment(
    public val note: Note? = null,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_noteListFragment_to_noteEditorFragment

    public override val arguments: Bundle
      @Suppress("CAST_NEVER_SUCCEEDS")
      get() {
        val result = Bundle()
        if (Parcelable::class.java.isAssignableFrom(Note::class.java)) {
          result.putParcelable("note", this.note as Parcelable?)
        } else if (Serializable::class.java.isAssignableFrom(Note::class.java)) {
          result.putSerializable("note", this.note as Serializable?)
        }
        return result
      }
  }

  public companion object {
    public fun actionNoteListFragmentToNoteEditorFragment(note: Note? = null): NavDirections =
        ActionNoteListFragmentToNoteEditorFragment(note)
  }
}
