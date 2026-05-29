package com.example.offlinenotes.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.offlinenotes.R
import com.example.offlinenotes.model.Schedule
import com.example.offlinenotes.model.ScheduleData
import com.google.gson.Gson

class ScheduleAdapter(private val listener: (Schedule) -> Unit) : ListAdapter<Schedule, ScheduleAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position), listener)
    }

    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tv_schedule_title)
        private val preview: TextView = itemView.findViewById(R.id.tv_schedule_preview)
        private val gson = Gson()

        fun bind(schedule: Schedule, listener: (Schedule) -> Unit) {
            title.text = schedule.name

            try {
                val data = gson.fromJson(schedule.gridData, ScheduleData::class.java)
                preview.text = "Grid: ${data.columns.size} Columns x ${data.rows.size} Rows"
            } catch (e: Exception) {
                preview.text = "Invalid Schedule Data"
            }

            itemView.setOnClickListener { listener(schedule) }
        }
    }

    class ScheduleDiffCallback : DiffUtil.ItemCallback<Schedule>() {
        override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem == newItem
    }
}