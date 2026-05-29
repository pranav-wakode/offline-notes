package com.example.offlinenotes.ui.adapter

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.offlinenotes.R
import com.example.offlinenotes.model.Folder
import com.google.android.material.card.MaterialCardView

class FolderAdapter(
    private val onFolderClick: (Folder?) -> Unit,
    private val onFolderLongClick: (Folder) -> Unit
) : ListAdapter<Folder, FolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    private var selectedFolderId: Int? = null

    fun setSelectedFolder(folderId: Int?) {
        val previousSelected = selectedFolderId
        selectedFolderId = folderId

        // Notify changes to update UI states (highlighting)
        val currentList = currentList
        val oldIndex = currentList.indexOfFirst { it.id == previousSelected }
        val newIndex = currentList.indexOfFirst { it.id == selectedFolderId }

        if (oldIndex != -1) notifyItemChanged(oldIndex + 1) // +1 because of "All Notes" root item
        if (newIndex != -1) notifyItemChanged(newIndex + 1)

        // Always notify position 0 ("All Notes") to toggle its state if switching to/from root
        notifyItemChanged(0)
    }

    override fun getItemCount(): Int {
        // +1 to account for the static "All Notes" (root) item at the beginning
        return super.getItemCount() + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_ROOT else VIEW_TYPE_FOLDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_chip, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        if (position == 0) {
            // Bind the "All Notes" root folder
            holder.bindRoot(selectedFolderId == null) {
                onFolderClick(null)
            }
        } else {
            // Bind dynamic folders
            val folder = getItem(position - 1)
            holder.bind(folder, selectedFolderId == folder.id, onFolderClick, onFolderLongClick)
        }
    }

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.card_folder)
        private val name: TextView = itemView.findViewById(R.id.tv_folder_name)

        fun bindRoot(isSelected: Boolean, onClick: () -> Unit) {
            name.text = "All Notes"
            setSelectionStyle(isSelected)
            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener { false } // No action on root long click
        }

        fun bind(folder: Folder, isSelected: Boolean, onClick: (Folder) -> Unit, onLongClick: (Folder) -> Unit) {
            name.text = folder.name
            setSelectionStyle(isSelected)
            itemView.setOnClickListener { onClick(folder) }
            itemView.setOnLongClickListener {
                onLongClick(folder)
                true
            }
        }

        private fun setSelectionStyle(isSelected: Boolean) {
            val context = itemView.context
            if (isSelected) {
                // Fetch primary color for selected state
                val typedValue = TypedValue()
                context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
                card.setCardBackgroundColor(typedValue.data)

                context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
                name.setTextColor(typedValue.data)
            } else {
                // Default unselected state
                card.setCardBackgroundColor(Color.TRANSPARENT)

                val typedValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                name.setTextColor(typedValue.data)
            }
        }
    }

    class FolderDiffCallback : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Folder, newItem: Folder) = oldItem == newItem
    }

    companion object {
        private const val VIEW_TYPE_ROOT = 0
        private const val VIEW_TYPE_FOLDER = 1
    }
}