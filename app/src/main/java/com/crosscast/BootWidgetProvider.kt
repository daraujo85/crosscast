package com.crosscast

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import java.io.File

class BootWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val views = buildRemoteViews(context, isTunnelRunning(context))
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun buildRemoteViews(context: Context, tunnelRunning: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_boot)

        // Cor e label baseadas no status
        if (tunnelRunning) {
            views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#22E27D"))
            views.setTextViewText(R.id.widget_status, "TUNNEL ON")
            views.setTextColor(R.id.widget_status, Color.BLACK)
        } else {
            views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#FF4545"))
            views.setTextViewText(R.id.widget_status, "TUNNEL OFF")
            views.setTextColor(R.id.widget_status, Color.WHITE)
        }

        // Click: abrir Termux com o script
        val clickIntent = Intent().apply {
            setClassName("com.termux", "com.termux.app.RunScriptActivity")
            putExtra("com.termux.RUN_SCRIPT_PATH", "/sdcard/termux-tunnel.sh")
        }
        val fallbackIntent = Intent().apply {
            setClassName("com.termux", "com.termux.app.TermuxActivity")
        }
        val pending = PendingIntent.getActivity(
            context, 0, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)

        return views
    }

    private fun isTunnelRunning(context: Context): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec("pidof cloudflared")
            proc.waitFor()
            proc.exitValue() == 0
        } catch (e: Exception) {
            // Fallback: verifica o arquivo de log mais recente
            val logFile = File("/data/data/com.termux/files/usr/var/log/sv/cloudflared/current")
            logFile.exists()
        }
    }
}
