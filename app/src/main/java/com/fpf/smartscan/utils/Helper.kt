package com.fpf.smartscan.utils

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.any

fun toEpochSeconds(dateString: String): Long {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val date = sdf.parse(dateString)!!
    return date.time / 1000
}

fun toEpochSeconds(year: Int, month: Int, day: Int): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(year, month, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis / 1000
}

fun toDateString(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDate(epochSeconds: Long?): String? {
    if (epochSeconds == null) return null
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(epochSeconds * 1000))
}

fun getTimeInMinutesAndSeconds(milliseconds: Long): Pair<Long, Long> {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    return Pair(minutes, seconds % 60)
}

fun isServiceRunning(context: Context, serviceClass: Class<out Service>): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return am.getRunningServices(Int.MAX_VALUE).any {
        it.service.className == serviceClass.name
    }
}

fun isConnectedToWifi(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}