package com.fpf.smartscan.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fpf.smartscan.notifications.NotificationChannels
import com.fpf.smartscan.notifications.showNotification

class ConceptReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getLongExtra(EXTRA_NOTIFICATION_ID, 0L).toInt()
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)

        showNotification(
            context = context,
            title = "Concept reminder",
            channelId = NotificationChannels.CONCEPT_REMINDERS,
            text = description,
            id = notificationId
        )
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val EXTRA_DESCRIPTION = "description"
    }
}