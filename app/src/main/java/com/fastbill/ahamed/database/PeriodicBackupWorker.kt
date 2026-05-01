package com.fastbill.ahamed.database

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log

class PeriodicBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sharedPrefs = applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val profileName = sharedPrefs.getString("active_profile_name", null)

        return if (!profileName.isNullOrEmpty()) {
            val backupManager = BackupManager(applicationContext)
            val result = backupManager.createAndUploadSnapshot(profileName)
            
            if (result.isSuccess) {
                Log.d("BackupWorker", "Periodic backup successful for $profileName")
                Result.success()
            } else {
                Log.e("BackupWorker", "Periodic backup failed: ${result.exceptionOrNull()?.message}")
                Result.retry()
            }
        } else {
            Log.w("BackupWorker", "No active profile name found. Skipping backup.")
            Result.failure()
        }
    }
}
