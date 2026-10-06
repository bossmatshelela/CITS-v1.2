package com.matshelela.childimmunisationtrackingsystemappv1.data

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

class FirestoreSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "FirestoreSyncWorker"
        
        /**
         * Schedules a one-time background sync task that runs as soon as network connectivity is restored.
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncWorkRequest)
            Log.d(TAG, "Successfully enqueued database sync background worker with connected network constraints.")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "WorkManager background sync triggered...")
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.immunisationDao()
            val syncRepository = FirestoreSyncRepository(dao)

            val result = syncRepository.syncAllUnsyncedRecords()

            if (result.isSuccess) {
                Log.d(TAG, "WorkManager synchronization complete. All offline records successfully linked to Firebase Firestore.")
                Result.success()
            } else {
                Log.w(TAG, "Synchronization completed with partial failures. Requesting WorkManager retry.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager background execution failed fatally: ${e.message}", e)
            Result.failure()
        }
    }
}
