package com.fpf.smartscan.utils

import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import android.Manifest
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fpf.smartscan.R
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
fun showNotification(context: Context, title: String, text: String, id: Int = 1001) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
    }
    val channelId = context.getString(R.string.worker_channel_id)
    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.smartscan_logo)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        notify(id, notificationBuilder.build())
    }
}

fun isServiceRunning(context: Context, serviceClass: Class<out Service>): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return am.getRunningServices(Int.MAX_VALUE).any {
        it.service.className == serviceClass.name
    }
}

fun createTrashRequest(context: Context, uris: Collection<Uri>, trash: Boolean = true): IntentSenderRequest {
    val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris.toList(), trash)
    return IntentSenderRequest.Builder(pendingIntent.intentSender).build()
}

fun isConnectedToWifi(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}