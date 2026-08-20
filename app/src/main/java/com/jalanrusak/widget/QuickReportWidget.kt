package com.jalanrusak.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jalanrusak.R
import com.jalanrusak.ui.overlay.QuickReportOverlay

class QuickReportWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget is removed
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (ACTION_CLICK == intent.action) {
            // Start the overlay activity
            val overlayIntent = Intent(context, QuickReportOverlay::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(overlayIntent)

            // Start the background service to submit the report
            val serviceIntent = Intent(context, QuickReportService::class.java).apply {
                action = QuickReportService.ACTION_SUBMIT_REPORT
            }
            context.startService(serviceIntent)
        }
    }

    companion object {
        private const val ACTION_CLICK = "com.jalanrusak.ACTION_WIDGET_CLICK"

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.quick_report_widget)

            // Create click intent
            val intent = Intent(context, QuickReportWidget::class.java).apply {
                action = ACTION_CLICK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
