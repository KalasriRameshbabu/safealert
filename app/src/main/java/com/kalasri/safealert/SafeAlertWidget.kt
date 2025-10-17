package com.kalasri.safealert

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class SafeAlertWidget : AppWidgetProvider() {

    companion object {
        const val WIDGET_BUTTON_CLICK = "com.kalasri.safealert.WIDGET_BUTTON_CLICK"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.safe_alert_widget)

            // Create an Intent to launch MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                action = WIDGET_BUTTON_CLICK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}