package com.fpf.smartscan.workers

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.any

suspend fun isWorkScheduled(context: Context, workName: String): Boolean {
    return withContext(Dispatchers.IO) {
        val workManager = WorkManager.getInstance(context)
        val workInfoList = workManager.getWorkInfosForUniqueWork(workName).get()
        workInfoList.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    }
}

fun cancelWorker(context: Context, uniqueWorkName: String?, tag: String?){
    val workManager = WorkManager.getInstance(context.applicationContext)
    uniqueWorkName?.let{workManager.cancelUniqueWork(it)}
    tag?.let{workManager.cancelAllWorkByTag(it)}
}

