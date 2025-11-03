package com.example.offlinenotes.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.offlinenotes.R
import com.example.offlinenotes.model.Note
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(private val listener: (Note) -> Unit) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note, listener)
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tv_note_title)
        private val content: TextView = itemView.findViewById(R.id.tv_note_content_preview)
        private val date: TextView = itemView.findViewById(R.id.tv_note_date)
        private val card: CardView = itemView.findViewById(R.id.note_card)
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(note: Note, listener: (Note) -> Unit) {
            title.text = note.title
            content.text = note.content
            date.text = dateFormat.format(Date(note.modifiedAt))
            
            // Here you could set note.color to card.setCardBackgroundColor()

            itemView.setOnClickListener { listener(note) }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}