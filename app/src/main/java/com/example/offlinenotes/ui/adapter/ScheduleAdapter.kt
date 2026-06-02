package com.example.offlinenotes.ui.adapter

import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
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
        private val previewText: TextView = itemView.findViewById(R.id.tv_schedule_preview_text)
        private val tablePreview: TableLayout = itemView.findViewById(R.id.table_preview)
        private val gson = Gson()

        fun bind(schedule: Schedule, listener: (Schedule) -> Unit) {
            title.text = schedule.name

            try {
                val data = gson.fromJson(schedule.gridData, ScheduleData::class.java)
                previewText.text = "${data.columns.size} Columns × ${data.rows.size} Rows"
                renderPreviewGrid(data)
            } catch (e: Exception) {
                previewText.text = "Invalid Schedule Data"
                tablePreview.removeAllViews()
            }

            itemView.setOnClickListener { listener(schedule) }
        }

        private fun renderPreviewGrid(data: ScheduleData) {
            tablePreview.removeAllViews()
            val context = itemView.context

            val typedValue = TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            val textColor = typedValue.data

            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true)
            val borderColor = typedValue.data

            // FIXED: Removed the 4x4 limit. Allow the HorizontalScrollView to do its job.
            // Header Row
            val headerRow = TableRow(context)
            headerRow.addView(createCell("", true, textColor, borderColor))
            for (c in 0 until data.columns.size) {
                headerRow.addView(createCell(data.columns[c], true, textColor, borderColor))
            }
            tablePreview.addView(headerRow)

            // Data Rows
            for (r in 0 until data.rows.size) {
                val row = TableRow(context)
                row.addView(createCell(data.rows[r], true, textColor, borderColor))

                for (c in 0 until data.columns.size) {
                    val key = "${r}_${c}"
                    val content = data.cells[key] ?: ""
                    val displayContent = if (content.length > 15) content.take(12) + "..." else content
                    row.addView(createCell(displayContent, false, textColor, borderColor))
                }
                tablePreview.addView(row)
            }
        }

        private fun createCell(text: String, isHeader: Boolean, textColor: Int, borderColor: Int): TextView {
            val tv = TextView(itemView.context)
            tv.text = text
            tv.setPadding(16, 12, 16, 12)
            tv.gravity = Gravity.CENTER
            tv.setTextColor(textColor)
            tv.textSize = 12f
            tv.maxLines = 1

            if (isHeader) {
                tv.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            return tv
        }
    }

    class ScheduleDiffCallback : DiffUtil.ItemCallback<Schedule>() {
        override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem == newItem
    }
}