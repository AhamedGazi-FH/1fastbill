package com.fastbill.ahamed.database

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RateSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Initialize Database and DAO
                val database = InvoiceDatabase.getDatabase(applicationContext)
                val rateDao = database.rateDao()
                
                val unsyncedRates = rateDao.getUnsyncedRates()
                if (unsyncedRates.isEmpty()) return@withContext Result.success()

                val db = FirebaseFirestore.getInstance()
                val batch = db.batch()
                val ratesCollection = db.collection("rates") // 🚀 Lowercase for consistency

                // Map Rate entity to Firestore objects using item_name as document ID
                for (rate in unsyncedRates) {
                    var safeDocId = rate.item_name.replace("/", "_").replace("\\", "_").trim()
                    if (safeDocId.isEmpty()) safeDocId = "unnamed_${System.currentTimeMillis()}"

                    val rateMap = hashMapOf(
                        "item_name" to rate.item_name,
                        "rate" to rate.rate,
                        "updatedAt" to rate.updatedAt
                    )
                    val docRef = ratesCollection.document(safeDocId)
                    batch.set(docRef, rateMap)
                }

                // Execute batch write
                batch.commit().await()

                // On Success: Mark as synced in local DB
                val syncedNames = unsyncedRates.map { it.item_name }
                rateDao.markRatesAsSynced(syncedNames)
                
                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }
}
