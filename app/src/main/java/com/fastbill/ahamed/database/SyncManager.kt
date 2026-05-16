package com.fastbill.ahamed.database

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class SyncManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val database = InvoiceDatabase.getDatabase(context)
    private val customerDao = database.customerDao()
    private val syncLogDao = database.syncLogDao()

    suspend fun forceSyncRatesNow(context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                val dbInstance = FirebaseFirestore.getInstance()
                val roomDb = InvoiceDatabase.getDatabase(context)
                val rateDao = roomDb.rateDao()

                // STEP 1: Handle pending deletions first
                val pendingDeletions = rateDao.getPendingDeletions()
                for (rate in pendingDeletions) {
                    try {
                        val safeDocId = rate.item_name.replace("/", "_").replace("\\", "_").trim()
                        dbInstance.collection("rates").document(safeDocId).delete().await()
                        rateDao.deleteRatePermanently(rate.item_name)
                    } catch (e: Exception) {
                        Log.e("SYNC_ENGINE", "Failed to delete rate from Firestore: ${e.message}")
                    }
                }

                // STEP 2: Push unsynced active rates
                val unsyncedRates = rateDao.getUnsyncedActiveRates()
                if (unsyncedRates.isNotEmpty()) {
                    val ratesCollection = dbInstance.collection("rates")
                    val chunks = unsyncedRates.chunked(400)
                    for (chunk in chunks) {
                        val batch = dbInstance.batch()
                        val validSyncedNames = mutableListOf<String>()
                        for (rate in chunk) {
                            var safeDocId = rate.item_name.replace("/", "_").replace("\\", "_").trim()
                            if (safeDocId.isEmpty()) safeDocId = "unnamed_${System.currentTimeMillis()}"
                            val rateMap = hashMapOf(
                                "item_name" to rate.item_name,
                                "rate" to rate.rate,
                                "updatedAt" to rate.updatedAt
                            )
                            batch.set(ratesCollection.document(safeDocId), rateMap)
                            validSyncedNames.add(rate.item_name)
                        }
                        batch.commit().await()
                        if (validSyncedNames.isNotEmpty()) {
                            rateDao.markRatesAsSynced(validSyncedNames)
                        }
                    }
                }

                // STEP 3: Pull from Firebase and smart merge
                val snapshot = dbInstance.collection("rates").get().await()
                val localRates = rateDao.getAllRatesSync().associateBy { it.item_name }
                val ratesToInsert = mutableListOf<Rate>()
                for (doc in snapshot.documents) {
                    val itemName = doc.getString("item_name") ?: continue
                    val rateVal = doc.getDouble("rate") ?: 0.0
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    val local = localRates[itemName]
                    if (local == null || (local.isSynced && !local.isDeleted)) {
                        ratesToInsert.add(Rate(item_name = itemName, rate = rateVal, updatedAt = updatedAt, isSynced = true))
                    }
                }
                if (ratesToInsert.isNotEmpty()) {
                    rateDao.insertAllRates(ratesToInsert)
                }

                "Deleted ${pendingDeletions.size}, Pushed ${unsyncedRates.size}, Pulled ${ratesToInsert.size} rates"

            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    fun pullRatesSilentlyOnStartup(context: Context) {
        // 🚀 EXPERT FIX: True background processing, Zero UI blocking
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = InvoiceDatabase.getDatabase(context)
                val rateDao = db.rateDao()
                
                // 1. Get all local rates to check sync status
                val localRates = rateDao.getAllRatesSync().associateBy { it.item_name }
                
                // 2. Fetch all rates from Firebase Firestore 'rates' collection
                val snapshot = FirebaseFirestore.getInstance().collection("rates").get().await()
                
                // 3. Smart Merge Logic
                val ratesToInsert = mutableListOf<Rate>()
                for (doc in snapshot.documents) {
                    val itemName = doc.getString("item_name") ?: continue
                    val rateVal = doc.getDouble("rate") ?: 0.0
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    
                    val cloudRate = Rate(item_name = itemName, rate = rateVal, updatedAt = updatedAt, isSynced = true)
                    val local = localRates[cloudRate.item_name]
                    
                    // If not in local, OR if local is already synced safely, we can update it from cloud
                    if (local == null || local.isSynced) {
                        ratesToInsert.add(
                            Rate(
                                item_name = cloudRate.item_name,
                                rate = cloudRate.rate,
                                updatedAt = cloudRate.updatedAt,
                                isSynced = true // It came from cloud, so it is synced
                            )
                        )
                    }
                }

                // 4. Bulk Insert the safe rates
                if (ratesToInsert.isNotEmpty()) {
                    rateDao.insertAllRates(ratesToInsert)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Silent fail. Do not crash the app or show toasts.
            }
        }
    }

    fun schedulePeriodicRateSync(context: Context) {
        // 🚀 EXPERT FIX: Strict battery and network constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<RateSyncWorker>(
            24, TimeUnit.HOURS, // Run once every 24 hours
            1, TimeUnit.HOURS   // Flex interval
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "RateAutoSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP, // Do not override if already scheduled
            syncRequest
        )
    }

    suspend fun pushUnsyncedCustomers(): Result<Unit> {
        addLog("SYNC_START", "Starting sync process")
        return try {
            // 1. Handle Deletions First
            val pendingDeletions = customerDao.getPendingDeletions()
            Log.d("SYNC_ENGINE", "Found ${pendingDeletions.size} pending deletions to process.")
            for (customer in pendingDeletions) {
                try {
                    db.collection("customers")
                        .document(customer.firestoreId)
                        .delete()
                        .await()
                    customerDao.deleteCustomerPermanently(customer.id)
                } catch (e: Exception) {
                    Log.e("SYNC_ENGINE", "Failed to delete customer from Firestore: ${e.message}")
                }
            }

            // 2. Handle Pushes (Optimized Payload)
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            Log.d("SYNC_ENGINE", "Found ${unsyncedCustomers.size} unsynced customers to push.")
            
            var count = 0
            for (customer in unsyncedCustomers) {
                try {
                    val fId = customer.customerName
                    // Optimized Payload: Only name, ID, and sync status. No Phone Number.
                    val customerMap = hashMapOf(
                        "customerName" to customer.customerName,
                        "firestoreId" to fId,
                        "isSynced" to true
                    )
                    
                    db.collection("customers")
                        .document(fId)
                        .set(customerMap)
                        .await()
                    
                    customerDao.updateCustomerAfterSync(customer.id, fId)
                    count++
                } catch (e: Exception) {
                    Log.e("SYNC_ENGINE", "Failed to push customer: ${e.message}")
                    throw e 
                }
            }
            addLog("PUSH_SUCCESS", "Synced deletions and pushed $count customers")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SyncManager", "Error in push/delete cycle", e)
            addLog("PUSH_ERROR", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchNewCustomers(): Result<Unit> {
        addLog("FETCH_START", "Starting fetch from Firestore")
        return try {
            val snapshot = db.collection("customers").get().await()
            val remoteIds = snapshot.documents.map { it.id }.toSet()
            
            val customers = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("customerName") ?: return@mapNotNull null
                val fId = doc.id
                // Use default phoneNumber as null for cloud-fetched customers
                Customer(customerName = name, phoneNumber = null, isSynced = true, firestoreId = fId)
            }
            
            if (customers.isNotEmpty()) {
                customerDao.insertMultipleCustomers(customers)
            }
            
            // Refactored deletion logic: Fetch local synced IDs and diff in Kotlin
            val localSyncedIds = customerDao.getAllSyncedFirestoreIds()
            val idsToDelete = localSyncedIds.filter { it !in remoteIds }
            
            if (idsToDelete.isNotEmpty()) {
                // Delete in safe chunks to avoid SQLite variable limit (999)
                val chunks = idsToDelete.chunked(900)
                chunks.forEach { chunk ->
                    customerDao.deleteCustomersByIds(chunk)
                }
            }
            
            addLog("FETCH_SUCCESS", "Fetched ${customers.size} customers, deleted ${idsToDelete.size} old ones")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SyncManager", "Error fetching customers", e)
            addLog("FETCH_ERROR", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun addLog(status: String, details: String) {
        syncLogDao.insert(SyncLog(timestamp = System.currentTimeMillis(), status = status, details = details))
    }
}
