package com.triplog.sync

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.triplog.worker.SyncWorker
import javax.inject.Inject
import javax.inject.Singleton

/** Asks for a sync to run as soon as the device is online. Safe to call as often as needed. */
interface SyncScheduler {
    fun requestSync()
}

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    // Lazy: WorkManager needs the Application, which is still being injected when this is built.
    private val workManager: dagger.Lazy<WorkManager>,
    private val config: SyncConfig
) : SyncScheduler {

    override fun requestSync() {
        if (!config.isConfigured) return
        // One named chain: a request made while a sync is running queues behind it instead of
        // starting a second worker that could upload the same records in parallel.
        workManager.get().enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            SyncWorker.buildOneTimeRequest()
        )
    }
}
