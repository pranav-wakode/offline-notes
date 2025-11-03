package com.example.offlinenotes.ui.fragments

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import com.example.offlinenotes.model.Note
import java.io.Serializable
import java.lang.UnsupportedOperationException
import kotlin.Suppress
import kotlin.jvm.JvmStatic

public data class NoteEditorFragmentArgs(
  public val note: Note? = null,
) : NavArgs {
  @Suppress("CAST_NEVER_SUCCEEDS")
  public fun toBundle(): Bundle {
    val result = Bundle()
    if (Parcelable::class.java.isAssignableFrom(Note::class.java)) {
      result.putParcelable("note", this.note as Parcelable?)
    } else if (Serializable::class.java.isAssignableFrom(Note::class.java)) {
      result.putSerializable("note", this.note as Serializable?)
    }
    return result
  }

  @Suppress("CAST_NEVER_SUCCEEDS")
  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    if (Parcelable::class.java.isAssignableFrom(Note::class.java)) {
      result.set("note", this.note as Parcelable?)
    } else if (Serializable::class.java.isAssignableFrom(Note::class.java)) {
      result.set("note", this.note as Serializable?)
    }
    return result
  }

  public companion object {
    @JvmStatic
    @Suppress("DEPRECATION")
    public fun fromBundle(bundle: Bundle): NoteEditorFragmentArgs {
      bundle.setClassLoader(NoteEditorFragmentArgs::class.java.classLoader)
      val __note : Note?
      if (bundle.containsKey("note")) {
        if (Parcelable::class.java.isAssignableFrom(Note::class.java) ||
            Serializable::class.java.isAssignableFrom(Note::class.java)) {
          __note = bundle.get("note") as Note?
        } else {
          throw UnsupportedOperationException(Note::class.java.name +
              " must implement Parcelable or Serializable or must be an Enum.")
        }
      } else {
        __note = null
      }
      return NoteEditorFragmentArgs(__note)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): NoteEditorFragmentArgs {
      val __note : Note?
      if (savedStateHandle.contains("note")) {
        if (Parcelable::class.java.isAssignableFrom(Note::class.java) ||
            Serializable::class.java.isAssignableFrom(Note::class.java)) {
          __note = savedStateHandle.get<Note?>("note")
        } else {
          throw UnsupportedOperationException(Note::class.java.name +
              " must implement Parcelable or Serializable or must be an Enum.")
        }
      } else {
        __note = null
      }
      return NoteEditorFragmentArgs(__note)
    }
  }
}
