package com.fastbill.ahamed.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleCustomers(customers: List<Customer>)

    @Query("SELECT * FROM customer_table ORDER BY customerName ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customer_table WHERE customerName LIKE :query OR phoneNumber LIKE :query")
    suspend fun searchCustomers(query: String): List<Customer>

    @Query("SELECT * FROM customer_table WHERE isSynced = 0")
    suspend fun getUnsyncedCustomers(): List<Customer>

    @Query("UPDATE customer_table SET isSynced = 1, firestoreId = :firestoreId WHERE id = :id")
    suspend fun updateCustomerAfterSync(id: Int, firestoreId: String)

    @Query("DELETE FROM customer_table WHERE isSynced = 1 AND firestoreId NOT IN (:remoteIds)")
    suspend fun deleteSyncedCustomersNotInRemote(remoteIds: List<String>)
}
