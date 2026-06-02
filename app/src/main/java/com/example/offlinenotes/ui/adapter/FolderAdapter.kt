package com.example.offlinenotes.ui.adapter

import android.graphics.Color
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
import com.google.android.material.color.MaterialColors

class FolderAdapter(
    private val onFolderClick: (Folder?) -> Unit,
    private val onFolderLongClick: (Folder) -> Unit
) : ListAdapter<Folder, FolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    private var selectedFolderId: Int? = null

    fun setSelectedFolder(folderId: Int?) {
        val previousSelected = selectedFolderId
        selectedFolderId = folderId

        val currentList = currentList
        val oldIndex = currentList.indexOfFirst { it.id == previousSelected }
        val newIndex = currentList.indexOfFirst { it.id == selectedFolderId }

        if (oldIndex != -1) notifyItemChanged(oldIndex + 1)
        if (newIndex != -1) notifyItemChanged(newIndex + 1)

        notifyItemChanged(0)
    }

    override fun getItemCount(): Int = super.getItemCount() + 1
    override fun getItemViewType(position: Int): Int = if (position == 0) VIEW_TYPE_ROOT else VIEW_TYPE_FOLDER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_chip, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        if (position == 0) {
            holder.bindRoot(selectedFolderId == null) { onFolderClick(null) }
        } else {
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
            itemView.setOnLongClickListener { false }
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
            // FIXED: Strictly enforce MaterialColors so it never turns invisible in dark mode
            val primaryColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorPrimary)
            val onPrimaryColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorOnPrimary)
            val surfaceVariant = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorSurfaceVariant)
            val onSurfaceVariant = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorOnSurfaceVariant)

            if (isSelected) {
                card.setCardBackgroundColor(primaryColor)
                name.setTextColor(onPrimaryColor)
                card.strokeWidth = 0
            } else {
                card.setCardBackgroundColor(surfaceVariant)
                name.setTextColor(onSurfaceVariant)
                card.strokeColor = onSurfaceVariant
                card.strokeWidth = 1
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