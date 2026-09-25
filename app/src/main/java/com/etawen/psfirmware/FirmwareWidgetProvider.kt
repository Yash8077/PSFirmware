package com.etawen.psfirmware

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Locale
import java.util.concurrent.Executors

class FirmwareWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_WIDGET, AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                refreshAll(context)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.etawen.psfirmware.ACTION_REFRESH_WIDGET"

        private val executor = Executors.newSingleThreadExecutor()

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_firmware)

            val locality = FirmwareStore.selectedLocality(context)
            val texts = CardTexts.create(
                context,
                locality?.locale ?: Locale.ENGLISH,
                locality?.nativeName,
            )
            texts.applyTo(views)

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, FirmwareWidgetProvider::class.java))
            if (ids.isEmpty()) return
            for (id in ids) {
                updateAppWidget(context, mgr, id)
            }
        }

        fun refreshDataAndWidgets(context: Context) {
            executor.execute {
                FirmwareUpdater.refreshIfStale(context, 60_000L)
                refreshAll(context)
            }
        }
    }
}
