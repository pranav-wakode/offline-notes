package com.example.offlinenotes.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.offlinenotes.R
import com.example.offlinenotes.model.Folder
import com.example.offlinenotes.model.Note
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(private val listener: (Note) -> Unit) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    private var folderList: List<Folder> = emptyList()

    fun setFolders(folders: List<Folder>) {
        folderList = folders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        // Find folder name dynamically
        val folderName = folderList.find { it.id == note.folderId }?.name
        holder.bind(note, folderName, listener)
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tv_note_title)
        private val content: TextView = itemView.findViewById(R.id.tv_note_content_preview)
        private val date: TextView = itemView.findViewById(R.id.tv_note_date)
        private val folderHint: TextView = itemView.findViewById(R.id.tv_note_folder_hint)

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(note: Note, folderName: String?, listener: (Note) -> Unit) {
            title.text = note.title
            content.text = note.content
            date.text = dateFormat.format(Date(note.modifiedAt))

            if (folderName != null) {
                folderHint.visibility = View.VISIBLE
                folderHint.text = folderName
            } else {
                folderHint.visibility = View.GONE
            }

            itemView.setOnClickListener { listener(note) }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }
}