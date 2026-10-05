package com.crosscast

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.Toast

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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.crosscast.WIDGET_TAP") {
            // Feedback visual imediato
            Toast.makeText(context, "Iniciando tunnel...", Toast.LENGTH_SHORT).show()

            // Dispara o boot receiver para iniciar tunnel
            val bootIntent = Intent(context, BootReceiver::class.java).apply {
                action = "com.crosscast.START_TUNNEL"
            }
            context.sendBroadcast(bootIntent)

            // Atualiza o widget após 3 segundos (tempo do cloudflared subir)
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.postDelayed({
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(ComponentName(context, BootWidgetProvider::class.java))
                for (id in ids) {
                    val views = buildRemoteViews(context, isTunnelRunning(context))
                    mgr.updateAppWidget(id, views)
                }
                val result = isTunnelRunning(context)
                val msg = if (result) "Tunnel ON" else "Falhou - abra Termux"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }, 3000)
        }
    }

    private fun buildRemoteViews(context: Context, tunnelRunning: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_boot)

        // Cor e label baseadas no status - troca o drawable do background
        if (tunnelRunning) {
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_circle_green)
            views.setTextViewText(R.id.widget_label, "ON")
            views.setTextColor(R.id.widget_label, Color.BLACK)
        } else {
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_circle_red)
            views.setTextViewText(R.id.widget_label, "OFF")
            views.setTextColor(R.id.widget_label, Color.WHITE)
        }

        // Click - broadcast interno
        val clickIntent = Intent(context, BootWidgetProvider::class.java).apply {
            action = "com.crosscast.WIDGET_TAP"
        }
        val pending = PendingIntent.getBroadcast(
            context, 0, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)

        return views
    }

    private fun isTunnelRunning(context: Context): Boolean {
        return try {
            val proc = ProcessBuilder("pidof", "cloudflared")
                .redirectErrorStream(true)
                .start()
            proc.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            proc.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }
}
