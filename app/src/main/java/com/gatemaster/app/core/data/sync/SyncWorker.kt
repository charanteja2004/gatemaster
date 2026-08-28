package com.gatemaster.app.core.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gatemaster.app.GateMasterApplication
import java.util.concurrent.TimeUnit

/**
 * Sync, off the main thread and outside the app's lifetime.
 *
 * WorkManager rather than a coroutine in a ViewModel, for the reason that
 * matters on a phone: the user finishes a mock test on the train, locks the
 * screen, and the app is killed before the upload lands. A ViewModel scope dies
 * with the screen; this survives the process and waits for a network.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as? GateMasterApplication)?.container
            ?: return Result.success()

        return when (val outcome = container.syncManager.sync()) {
            is SyncOutcome.Success -> {
                Log.i(
                    TAG,
                    "Synced: ${outcome.attemptsUploaded} up, ${outcome.attemptsDownloaded} down",
                )
                Result.success()
            }

            // Nothing to do and nothing wrong: not signed in, or no server.
            SyncOutcome.NothingToDo -> Result.success()

            // Retrying will not help until the user signs in again, and a
            // failed periodic job would keep its backoff climbing for nothing.
            SyncOutcome.SignedOut -> Result.success()

            is SyncOutcome.Retry -> {
                Log.i(TAG, "Sync deferred: ${outcome.reason}")
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val PERIODIC = "gatemaster-sync-periodic"
        private const val ONE_OFF = "gatemaster-sync-now"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * The background schedule. Six hours, which is not a compromise: study
         * progress is not urgent, and a tighter loop would wake the radio for
         * a payload measured in kilobytes.
         *
         * KEEP, so an existing schedule survives a relaunch rather than having
         * its period restart every time the app opens -- which, with a
         * six-hour period and an app opened daily, would mean it never ran.
         */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }

        /**
         * Sync as soon as there is a network -- after finishing a test, or when
         * the user asks.
         *
         * REPLACE, because two of these queued together would do the same work
         * twice; the later request is the one that knows about the newer data.
         */
        fun syncNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_OFF,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }
    }
}
