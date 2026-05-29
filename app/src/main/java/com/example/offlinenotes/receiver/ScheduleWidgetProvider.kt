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

        // Launch app on title click
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

        // Fetch data asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            val database = NoteDatabase.getDatabase(context)
            val schedules = database.getScheduleDao().getAllSchedulesSync() // Requires DAO sync method we created in Part 3

            withContext(Dispatchers.Main) {
                if (schedules.isNotEmpty()) {
                    // Grab the most recently modified schedule
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
        }
    }

    private fun buildGridUI(context: Context, views: RemoteViews, gridDataString: String) {
        views.removeAllViews(R.id.widget_grid_container)

        try {
            val data = Gson().fromJson(gridDataString, ScheduleData::class.java)

            // Header Row
            val headerRow = RemoteViews(context.packageName, android.R.layout.widget_layout_horizontal) // Using native horizontal layout
            headerRow.addView(android.R.id.content, createCellRemoteView(context, "", true))

            data.columns.forEach { colName ->
                headerRow.addView(android.R.id.content, createCellRemoteView(context, colName, true))
            }
            views.addView(R.id.widget_grid_container, headerRow)

            // Data Rows
            data.rows.forEachIndexed { rowIndex, rowName ->
                val dataRow = RemoteViews(context.packageName, android.R.layout.widget_layout_horizontal)
                dataRow.addView(android.R.id.content, createCellRemoteView(context, rowName, true))

                data.columns.forEachIndexed { colIndex, _ ->
                    val key = "${rowIndex}_${colIndex}"
                    val content = data.cells[key] ?: ""
                    dataRow.addView(android.R.id.content, createCellRemoteView(context, content, false))
                }
                views.addView(R.id.widget_grid_container, dataRow)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createCellRemoteView(context: Context, text: String, isHeader: Boolean): RemoteViews {
        // We inject standard TextViews directly into the remote hierarchy dynamically
        val cell = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        cell.setTextViewText(android.R.id.text1, text)
        cell.setTextColor(android.R.id.text1, Color.BLACK)

        if (isHeader) {
            cell.setInt(android.R.id.text1, "setBackgroundColor", Color.parseColor("#E0E0E0"))
        } else {
            cell.setInt(android.R.id.text1, "setBackgroundColor", Color.parseColor("#F5F5F5"))
        }

        // Add padding via RemoteViews margin trick or standard padding if API allows.
        // Using simple_list_item_1 provides default sensible paddings.
        return cell
    }
}