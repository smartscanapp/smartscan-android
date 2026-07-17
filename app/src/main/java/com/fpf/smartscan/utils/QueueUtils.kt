package com.fpf.smartscan.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class Queue<JobData>(
    concurrency: Int = 4,
    private val scope: CoroutineScope,
    private val onProcess: suspend (jobData: JobData, workerId: Int) -> Unit

) {

    private val queue = Channel<JobData>(Channel.UNLIMITED)

    init {
        repeat(concurrency) { workerId ->
            scope.launch {
                for (task in queue) {
                    processTask(task, workerId)
                }
            }
        }
    }

    suspend fun submit(jobData: JobData) {
        queue.send(jobData)
    }

    private suspend fun processTask(jobData: JobData, workerId: Int) {
        onProcess(jobData, workerId)
    }
}