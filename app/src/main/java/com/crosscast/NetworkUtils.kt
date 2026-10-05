package com.crosscast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.text.format.Formatter
import java.net.Inet4Address
import java.net.NetworkInterface

fun getBestLocalIpAddress(context: Context): String {
    return try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return "0.0.0.0"
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "0.0.0.0"

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
            if (!ip.isNullOrBlank() && ip != "0.0.0.0") return ip
        }

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces.asSequence()) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses.asSequence()) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress
                        if (ip != null && !ip.startsWith("127.") && !ip.startsWith("100.")) return ip
                    }
                }
            }
        }

        "0.0.0.0"
    } catch (e: Exception) {
        "0.0.0.0"
    }
}
