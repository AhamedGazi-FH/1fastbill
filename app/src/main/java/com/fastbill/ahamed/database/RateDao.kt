package com.fastbill.ahamed.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: Rate)

    @Query("SELECT * FROM rate_table WHERE item_name = :itemName LIMIT 1")
    suspend fun getRateByItemName(itemName: String): Rate?

    @Update
    suspend fun update(rate: Rate)

    // 🚀 EXPERT FIX: Sync Queries
    @Query("SELECT * FROM rate_table WHERE isSynced = 0")
    suspend fun getUnsyncedRates(): List<Rate>

    @Query("UPDATE rate_table SET isSynced = 1 WHERE item_name IN (:itemNames)")
    suspend fun markRatesAsSynced(itemNames: List<String>)

    // 🚀 EXPERT FIX: Bulk insert for fast startup sync
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRates(rates: List<Rate>)

    @Query("SELECT * FROM rate_table")
    suspend fun getAllRatesSync(): List<Rate>

    @Query("SELECT * FROM rate_table WHERE isDeleted = 0 ORDER BY item_name ASC")
    fun getAllRates(): Flow<List<Rate>>

    @Query("DELETE FROM rate_table WHERE item_name = :itemName")
    suspend fun deleteRate(itemName: String)

    @Query("DELETE FROM rate_table WHERE isSynced = 1")
    suspend fun clearAllSyncedRatesLocally()

    @Query("UPDATE rate_table SET isDeleted = 1 WHERE item_name = :itemName")
    suspend fun markRateAsDeleted(itemName: String)

    @Query("SELECT * FROM rate_table WHERE isDeleted = 1 AND isSynced = 1")
    suspend fun getPendingDeletions(): List<Rate>

    @Query("DELETE FROM rate_table WHERE item_name = :itemName")
    suspend fun deleteRatePermanently(itemName: String)

    @Query("SELECT * FROM rate_table WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedActiveRates(): List<Rate>
}