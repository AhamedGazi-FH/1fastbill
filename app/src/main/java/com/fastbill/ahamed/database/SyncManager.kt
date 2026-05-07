package com.fastbill.ahamed.database

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SyncManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val database = InvoiceDatabase.getDatabase(context)
    private val customerDao = database.customerDao()
    private val syncLogDao = database.syncLogDao()

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
