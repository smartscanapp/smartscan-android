package com.fpf.smartscan.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.work.*
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.core.concepts.ConceptManager
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.models.ModelRepository
import com.fpf.smartscan.core.search.getRecentSearches
import com.fpf.smartscan.receivers.ConceptReminderReceiver
import com.fpf.smartscansdk.core.embeddings.embedBatch
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.getValue

class ConceptsReminderWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), KoinComponent {

    companion object {
        const val TAG = "ConceptsReminderWorker"
        private const val REMINDER_HOUR = "reminder_hour"
        private const val REMINDER_MINUTE = "reminder_minute"

        private const val CANDIDATES_LIMIT = 7 // 1 per day of the week

        private const val RECENT_REMINDERS_LIMIT = 28

        fun scheduleWorker(context: Context, frequency: Pair<Long, TimeUnit>, reminderTime: Pair<Int, Int>?=null, delay: Pair<Long, TimeUnit>? = null) {
            val inputData = workDataOf(
                REMINDER_HOUR to reminderTime?.first,
                REMINDER_MINUTE to reminderTime?.second
            )

            val workRequestBuilder = PeriodicWorkRequestBuilder<ConceptsReminderWorker>(frequency.first, frequency.second)
                .setInputData(inputData)

            if (delay != null) workRequestBuilder.setInitialDelay(delay.first, delay.second)

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequestBuilder.build()
            )
        }
    }

    private val modelRepository: ModelRepository by inject()
    private val textEmbedder by lazy { modelRepository.getMiniLmTextEmbedder() }
    private val mediaMetadataRepository: MediaMetadataRepository by inject()
    private val conceptManager: ConceptManager by inject()
    private val sharedPrefs: SharedPreferences by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if(!conceptManager.exists) return@withContext Result.success()
            if(!textEmbedder.isInitialized()) textEmbedder.initialize()

            val reminderHour = inputData.getInt(REMINDER_HOUR, 8)
            val reminderMinute = inputData.getInt(REMINDER_MINUTE, 30)
            val searches = getRecentSearches(sharedPrefs, PrefsKeys.RECENT_SEARCHES_KEY)
            val recentSearchesEmbeds = embedBatch(applicationContext, textEmbedder, searches).map { it.toQInt8Embed() }

            val recentReminders = getRecentReminders()
            val candidates = conceptManager.getReminderCandidates(recentSearchesEmbeds, recentReminders = recentReminders.toSet(), topN = CANDIDATES_LIMIT)
            val (imageCandidates, videoCandidates) = candidates.partition { it.second == MediaType.IMAGE }
            val mediaCandidates = mutableListOf<MediaMetadata>()
            mediaCandidates.addAll(mediaMetadataRepository.getByIds(imageCandidates.map { it.first }, MediaType.IMAGE))
            mediaCandidates.addAll(mediaMetadataRepository.getByIds(videoCandidates.map { it.first }, MediaType.VIDEO))

            mediaCandidates.filter { it.description != null }.forEachIndexed { index, media ->
                scheduleReminder(applicationContext, getReminderTriggerTime(index, reminderHour, reminderMinute), media.description!!, media.id.hashCode())
            }

            recentReminders.addAll(candidates)
            saveRecentReminders(recentReminders.takeLast(RECENT_REMINDERS_LIMIT))

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ConceptsReminderWorker", e)
            Result.failure()
        }
    }

    private fun scheduleReminder(context: Context, triggerAtMillis: Long, description: String, requestCode: Int) {
        val intent = Intent(context, ConceptReminderReceiver::class.java).apply {
            putExtra(ConceptReminderReceiver.EXTRA_DESCRIPTION, description)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService<AlarmManager>()?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun getReminderTriggerTime(index: Int, hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var trigger = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)
        return trigger.plusDays(index.toLong()).toInstant().toEpochMilli()
    }

    private fun getRecentReminders(): MutableList<Pair<Long, MediaType>>{
        val recentRemindersStr = sharedPrefs.getString(PrefsKeys.RECENT_CONCEPT_REMINDERS, null)?: return mutableListOf()
        return Json.decodeFromString<List<Pair<Long, MediaType>>>(recentRemindersStr).toMutableList()
    }

    private fun saveRecentReminders(candidates: List<Pair<Long, MediaType>>){
        sharedPrefs.edit { putString(PrefsKeys.RECENT_CONCEPT_REMINDERS, Json.encodeToString<List<Pair<Long, MediaType>>>(candidates)) }
    }
}