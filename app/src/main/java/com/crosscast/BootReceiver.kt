package com.crosscast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.crosscast.START_TUNNEL" -> startTunnel()
            "com.crosscast.REFRESH_WIDGET" -> refreshWidget(context)
        }
    }

    private fun startTunnel() {
        // Tenta varias formas de iniciar o tunnel
        val attempts = listOf(
            // 1) Termux:API RunScriptActivity
            arrayOf("am", "start", "-n", "com.termux/com.termux.app.RunScriptActivity",
                "-e", "com.termux.RUN_SCRIPT_PATH", "/sdcard/termux-tunnel.sh"),
            // 2) Fallback - abrir Termux normal
            arrayOf("am", "start", "-n", "com.termux/com.termux.app.TermuxActivity")
        )

        for (cmd in attempts) {
            try {
                val proc = ProcessBuilder(*cmd)
                    .redirectErrorStream(true)
                    .start()
                proc.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                if (proc.exitValue() == 0) {
                    Log.i("BootReceiver", "Tunnel start command sent: ${cmd.joinToString(" ")}")
                    return
                }
            } catch (e: Exception) {
                Log.w("BootReceiver", "Failed: ${cmd.joinToString(" ")}: ${e.message}")
            }
        }
    }

    private fun refreshWidget(context: Context) {
        val intent = Intent(context, BootWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = android.appwidget.AppWidgetManager.getInstance(context)
                .getAppWidgetIds(android.content.ComponentName(context, BootWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}
