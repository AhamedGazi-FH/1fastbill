package com.fastbill.ahamed.database

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.fastbill.ahamed.model.AppBackupSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BackupManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val database = InvoiceDatabase.getDatabase(context)
    private val invoiceDao = database.invoiceDao()
    private val itemDao = database.itemDao()
    private val discountDao = database.discountDao()
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    suspend fun createAndUploadSnapshot(profileName: String): Result<Unit> {
        return try {
            val allInvoices = withContext(Dispatchers.IO) {
                invoiceDao.getAllInvoices()
            }

            val allItems = withContext(Dispatchers.IO) {
                itemDao.getAllItems()
            }

            val allDiscounts = withContext(Dispatchers.IO) {
                discountDao.getAllDiscounts()
            }
            
            val safeSettingsMap = mutableMapOf<String, Any>()
            for ((key, value) in sharedPrefs.all) {
                if (value is Set<*>) {
                    safeSettingsMap[key] = value.toList()
                } else if (value != null) {
                    safeSettingsMap[key] = value
                }
            }

            val snapshot = AppBackupSnapshot(
                profileName = profileName,
                timestamp = System.currentTimeMillis(),
                billsList = allInvoices,
                billItemsList = allItems,
                discountList = allDiscounts,
                settingsMap = safeSettingsMap
            )

            db.collection("backups")
                .document(profileName)
                .set(snapshot)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableProfilesWithDates(): Result<List<Pair<String, Long>>> {
        return try {
            val snapshot = db.collection("backups").get().await()
            val profiles = snapshot.documents.mapNotNull { doc ->
                val name = doc.id
                val timestamp = doc.getLong("timestamp") ?: 0L
                name to timestamp
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableProfiles(): Result<List<String>> {
        return try {
            val snapshot = db.collection("backups").get().await()
            val profiles = snapshot.documents.map { it.id }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadSnapshot(profileName: String): AppBackupSnapshot? {
        return try {
            val doc = db.collection("backups").document(profileName).get().await()
            doc.toObject(AppBackupSnapshot::class.java)
        } catch (e: Exception) {
            Log.e("BACKUP_RESTORE", "Error downloading snapshot: ${e.message}", e)
            null
        }
    }

    suspend fun applySelectiveRestore(snapshot: AppBackupSnapshot, restoreBills: Boolean, restoreSettings: Boolean) {
        withContext(Dispatchers.IO) {
            if (restoreBills) {
                // Merge data: Insert without deleting existing records. 
                // Room will handle conflicts based on OnConflictStrategy.REPLACE in the DAO.
                invoiceDao.insertMultipleInvoices(snapshot.billsList)
                itemDao.insertAll(snapshot.billItemsList)
            }

            if (restoreSettings) {
                // Restore Discount List (Room Entity)
                if (snapshot.discountList.isNotEmpty()) {
                    discountDao.insertAll(snapshot.discountList)
                }

                Log.d("BACKUP_RESTORE", "Downloaded settings count: ${snapshot.settingsMap.size}")
                val editor = sharedPrefs.edit()
                editor.clear()
                snapshot.settingsMap.forEach { (key, value) ->
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is List<*> -> {
                            // Android SharedPreferences requires a strict HashSet to save correctly
                            val stringSet = java.util.HashSet<String>(value.map { it.toString() })
                            editor.putStringSet(key, stringSet)
                        }
                        is Number -> {
                            val existingValue = sharedPrefs.all[key]
                            if (existingValue is Long) {
                                editor.putLong(key, value.toLong())
                            } else if (existingValue is Float || value is Double) {
                                editor.putFloat(key, value.toFloat())
                            } else {
                                editor.putInt(key, value.toInt())
                            }
                        }
                    }
                }
                val isCommitted = editor.commit()
                Log.d("BACKUP_RESTORE", "Settings commit successful: $isCommitted")
            }
        }
    }
}
