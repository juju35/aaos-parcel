package me.jstephan.parcel.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import me.jstephan.parcel.Prefs
import me.jstephan.parcel.data.DeliveryRepository
import me.jstephan.parcel.data.ParcelException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The hourly check: fetches the deliveries; the repository notifies those Parcel has news about. Runs while the car
 * is on and connected; WorkManager keeps the schedule across restarts.
 */
class CheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        if (!prefs.hasKey) return Result.success()
        return try {
            DeliveryRepository.get(applicationContext).refresh()
            prefs.authFailureNotified = false
            Result.success()
        } catch (e: ParcelException.Auth) {
            Log.w(TAG, "API key refused", e)
            if (prefs.notifications && !prefs.authFailureNotified) {
                Notifier.authFailed(applicationContext)
                prefs.authFailureNotified = true
            }
            Result.success()
        } catch (e: ParcelException.RateLimit) {
            // Retrying would only use up more of the hour's 20 requests: next run.
            Log.w(TAG, "Rate limited", e)
            Result.success()
        } catch (e: IOException) {
            Log.w(TAG, "Check failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CheckWorker"
        private const val NAME = "parcel-check"

        /** Every hour, with a network; keeps the current schedule if there is one. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CheckWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
