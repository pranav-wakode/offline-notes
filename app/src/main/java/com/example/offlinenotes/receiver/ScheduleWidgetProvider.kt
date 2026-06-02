package com.example.offlinenotes.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.offlinenotes.R
import com.example.offlinenotes.data.database.NoteDatabase
import com.example.offlinenotes.model.ScheduleData
import com.example.offlinenotes.ui.activity.MainActivity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_schedule)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

        // FIXED: Tell the OS this receiver is doing background work so it doesn't kill it prematurely
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = NoteDatabase.getDatabase(context)
                val schedules = database.getScheduleDao().getAllSchedulesSync()

                withContext(Dispatchers.Main) {
                    if (schedules.isNotEmpty()) {
                        val latestSchedule = schedules.maxByOrNull { it.modifiedAt }
                        latestSchedule?.let {
                            views.setTextViewText(R.id.widget_title, "🗓️ ${it.name}")
                            buildGridUI(context, views, it.gridData)
                        }
                    } else {
                        views.setTextViewText(R.id.widget_title, "No Schedules Found")
                        views.removeAllViews(R.id.widget_grid_container)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildGridUI(context: Context, views: RemoteViews, gridDataString: String) {
        views.removeAllViews(R.id.widget_grid_container)

        try {
            val data = Gson().fromJson(gridDataString, ScheduleData::class.java)

            // Limit widget grid to 4x4 so it fits cleanly without scrolling
            val maxCols = minOf(data.columns.size, 4)
            val maxRows = minOf(data.rows.size, 4)

            val headerRow = RemoteViews(context.packageName, R.layout.widget_row)
            headerRow.addView(R.id.widget_row_container, createCellRemoteView(context, "", true))

            for (c in 0 until maxCols) {
                val colName = if (c == 3 && data.columns.size > 4) "..." else data.columns[c]
                headerRow.addView(R.id.widget_row_container, createCellRemoteView(context, colName, true))
            }
            views.addView(R.id.widget_grid_container, headerRow)

            for (r in 0 until maxRows) {
                val dataRow = RemoteViews(context.packageName, R.layout.widget_row)
                val rowName = if (r == 3 && data.rows.size > 4) "..." else data.rows[r]
                dataRow.addView(R.id.widget_row_container, createCellRemoteView(context, rowName, true))

                for (c in 0 until maxCols) {
                    if (r == 3 && data.rows.size > 4 || c == 3 && data.columns.size > 4) {
                        dataRow.addView(R.id.widget_row_container, createCellRemoteView(context, "...", false))
                    } else {
                        val key = "${r}_${c}"
                        var content = data.cells[key] ?: ""
                        if (content.length > 8) content = content.take(6) + ".." // keep widget cells small
                        dataRow.addView(R.id.widget_row_container, createCellRemoteView(context, content, false))
                    }
                }
                views.addView(R.id.widget_grid_container, dataRow)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createCellRemoteView(context: Context, text: String, isHeader: Boolean): RemoteViews {
        val cell = RemoteViews(context.packageName, R.layout.widget_cell)
        cell.setTextViewText(R.id.widget_cell_text, text)

        if (isHeader) {
            cell.setInt(R.id.widget_cell_text, "setBackgroundColor", Color.parseColor("#E0E0E0"))
        } else {
            cell.setInt(R.id.widget_cell_text, "setBackgroundColor", Color.parseColor("#F5F5F5"))
        }

        return cell
    }
}