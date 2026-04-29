package com.fastbill.ahamed.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: Rate)

    @Query("SELECT * FROM rate_table WHERE item_name = :itemName LIMIT 1")
    suspend fun getRateByItemName(itemName: String): Rate?

    @Update
    suspend fun update(rate: Rate)
}