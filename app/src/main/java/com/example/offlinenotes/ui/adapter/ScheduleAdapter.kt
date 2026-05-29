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

            // Fetch theme colors for borders and text
            val typedValue = TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            val textColor = typedValue.data

            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true)
            val borderColor = typedValue.data

            // Limit preview to 4 rows and 4 columns to keep the list performant and clean
            val maxCols = minOf(data.columns.size, 4)
            val maxRows = minOf(data.rows.size, 4)

            // Header Row
            val headerRow = TableRow(context)
            headerRow.addView(createCell("", true, textColor, borderColor)) // Top-left empty
            for (c in 0 until maxCols) {
                val colName = if (c == 3 && data.columns.size > 4) "..." else data.columns[c]
                headerRow.addView(createCell(colName, true, textColor, borderColor))
            }
            tablePreview.addView(headerRow)

            // Data Rows
            for (r in 0 until maxRows) {
                val row = TableRow(context)
                val rowName = if (r == 3 && data.rows.size > 4) "..." else data.rows[r]
                row.addView(createCell(rowName, true, textColor, borderColor))

                for (c in 0 until maxCols) {
                    if (r == 3 && data.rows.size > 4 || c == 3 && data.columns.size > 4) {
                        row.addView(createCell("...", false, textColor, borderColor))
                    } else {
                        val key = "${r}_${c}"
                        val content = data.cells[key] ?: ""
                        // Truncate long content for preview
                        val displayContent = if (content.length > 15) content.take(12) + "..." else content
                        row.addView(createCell(displayContent, false, textColor, borderColor))
                    }
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

            // Programmatic border using a ShapeDrawable or simple background manipulation
            // For a fast preview, we just use margins inside the TableRow dividers
            return tv
        }
    }

    class ScheduleDiffCallback : DiffUtil.ItemCallback<Schedule>() {
        override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule) = oldItem == newItem
    }
}