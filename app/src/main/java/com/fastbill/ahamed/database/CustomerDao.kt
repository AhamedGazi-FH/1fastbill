package com.fastbill.ahamed.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleCustomers(customers: List<Customer>)

    @Query("SELECT * FROM customer_table WHERE isDeleted = 0 ORDER BY customerName ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customer_table WHERE isDeleted = 0 AND (customerName LIKE :query OR phoneNumber LIKE :query)")
    suspend fun searchCustomers(query: String): List<Customer>

    @Query("SELECT * FROM customer_table WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedCustomers(): List<Customer>

    @Query("UPDATE customer_table SET isSynced = 1, firestoreId = :firestoreId WHERE id = :id")
    suspend fun updateCustomerAfterSync(id: Int, firestoreId: String)

    @Query("SELECT firestoreId FROM customer_table WHERE isSynced = 1 AND firestoreId != ''")
    suspend fun getAllSyncedFirestoreIds(): List<String>

    @Query("DELETE FROM customer_table WHERE firestoreId IN (:ids)")
    suspend fun deleteCustomersByIds(ids: List<String>)

    @Query("SELECT * FROM customer_table WHERE isDeleted = 1 AND isSynced = 1")
    suspend fun getPendingDeletions(): List<Customer>

    @Query("DELETE FROM customer_table WHERE id = :id")
    suspend fun deleteCustomerPermanently(id: Int)

    @Query("UPDATE customer_table SET isDeleted = 1 WHERE id = :id")
    suspend fun markAsDeleted(id: Int)

    @Query("DELETE FROM customer_table WHERE isSynced = 1")
    suspend fun clearAllSyncedCustomersLocally()
}
