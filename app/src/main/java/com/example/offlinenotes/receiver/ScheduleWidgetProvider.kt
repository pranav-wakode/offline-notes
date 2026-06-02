package com.example.offlinenotes.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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

        // Detect Dark/Light Mode dynamically
        val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val bgColor = if (isNightMode) Color.parseColor("#121212") else Color.parseColor("#FFFFFF")
        val headerColor = if (isNightMode) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")
        val cellColor = if (isNightMode) Color.parseColor("#1E1E1E") else Color.parseColor("#F5F5F5")
        val textColor = if (isNightMode) Color.WHITE else Color.BLACK
        val titleBgColor = if (isNightMode) Color.parseColor("#00497D") else Color.parseColor("#0061A4")

        // Apply theme to root and title
        views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)
        views.setInt(R.id.widget_title, "setBackgroundColor", titleBgColor)

        try {
            val data = Gson().fromJson(gridDataString, ScheduleData::class.java)

            // Limit widget grid to 4x4 so it fits cleanly
            val maxCols = minOf(data.columns.size, 4)
            val maxRows = minOf(data.rows.size, 4)

            val headerRow = RemoteViews(context.packageName, R.layout.widget_row)
            headerRow.addView(R.id.widget_row_container, createCellRemoteView(context, "", headerColor, textColor, true))

            for (c in 0 until maxCols) {
                val colName = if (c == 3 && data.columns.size > 4) "..." else data.columns[c]
                headerRow.addView(R.id.widget_row_container, createCellRemoteView(context, colName, headerColor, textColor, true))
            }
            views.addView(R.id.widget_grid_container, headerRow)

            for (r in 0 until maxRows) {
                val dataRow = RemoteViews(context.packageName, R.layout.widget_row)
                val rowName = if (r == 3 && data.rows.size > 4) "..." else data.rows[r]
                dataRow.addView(R.id.widget_row_container, createCellRemoteView(context, rowName, headerColor, textColor, true))

                for (c in 0 until maxCols) {
                    if (r == 3 && data.rows.size > 4 || c == 3 && data.columns.size > 4) {
                        dataRow.addView(R.id.widget_row_container, createCellRemoteView(context, "...", cellColor, textColor, false))
                    } else {
                        val key = "${r}_${c}"
                        val content = data.cells[key] ?: ""
                        dataRow.addView(R.id.widget_row_container, createCellRemoteView(context, content, cellColor, textColor, false))
                    }
                }
                views.addView(R.id.widget_grid_container, dataRow)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createCellRemoteView(context: Context, text: String, bgColor: Int, textColor: Int, isHeader: Boolean): RemoteViews {
        val cell = RemoteViews(context.packageName, R.layout.widget_cell)
        cell.setTextViewText(R.id.widget_cell_text, text)
        cell.setTextColor(R.id.widget_cell_text, textColor)
        cell.setInt(R.id.widget_cell_text, "setBackgroundColor", bgColor)
        return cell
    }
}