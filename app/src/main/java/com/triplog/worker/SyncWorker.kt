package com.triplog.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.data.repository.TripRepository
import com.triplog.sync.SyncConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val tripRepository: TripRepository,
    private val fuelExpenseRepository: FuelExpenseRepository,
    private val syncConfig: SyncConfig
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Nothing to talk to yet: leave the records pending and do not burn retries.
        if (!syncConfig.isConfigured) return Result.success()

        return try {
            val result = tripRepository.syncPendingTrips() + fuelExpenseRepository.syncPendingExpenses()
            when {
                !result.shouldRetry -> Result.success()
                runAttemptCount < MAX_ATTEMPTS -> Result.retry()
                else -> Result.failure()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "TripLogSyncWorker"
        const val PERIODIC_WORK_NAME = "TripLogPeriodicSync"
        private const val MAX_ATTEMPTS = 5

        fun buildOneTimeRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

        fun buildPeriodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
    }
}
