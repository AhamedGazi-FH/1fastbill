package com.fastbill.ahamed.database

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.util.UUID

class SyncManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val database = InvoiceDatabase.getDatabase(context)
    private val customerDao = database.customerDao()
    private val syncLogDao = database.syncLogDao()

    suspend fun pushUnsyncedCustomers(): Result<Unit> {
        addLog("PUSH_START", "Starting push of unsynced customers")
        return try {
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            var count = 0
            for (customer in unsyncedCustomers) {
                val fId = if (customer.firestoreId.isEmpty()) UUID.randomUUID().toString() else customer.firestoreId
                val customerMap = hashMapOf(
                    "customerName" to customer.customerName,
                    "phoneNumber" to customer.phoneNumber,
                    "isSynced" to true,
                    "firestoreId" to fId
                )
                
                db.collection("customers")
                    .document(fId)
                    .set(customerMap)
                    .await()
                
                customerDao.updateCustomerAfterSync(customer.id, fId)
                count++
            }
            addLog("PUSH_SUCCESS", "Successfully pushed $count customers")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SyncManager", "Error pushing customers", e)
            addLog("PUSH_ERROR", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchNewCustomers(): Result<Unit> {
        addLog("FETCH_START", "Starting fetch from Firestore")
        return try {
            val snapshot = db.collection("customers").get().await()
            val remoteIds = snapshot.documents.map { it.id }
            
            val customers = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("customerName") ?: return@mapNotNull null
                val phone = doc.getString("phoneNumber")
                val fId = doc.id
                Customer(customerName = name, phoneNumber = phone, isSynced = true, firestoreId = fId)
            }
            
            if (customers.isNotEmpty()) {
                customerDao.insertMultipleCustomers(customers)
            }
            
            // Delete local synced records that are no longer in Firestore
            customerDao.deleteSyncedCustomersNotInRemote(remoteIds)
            
            addLog("FETCH_SUCCESS", "Fetched ${customers.size} customers and handled deletions")
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
