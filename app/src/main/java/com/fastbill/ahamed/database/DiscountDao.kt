package com.fastbill.ahamed.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DiscountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discount: Discount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(discounts: List<Discount>)

    @Query("SELECT * FROM discount_table WHERE id = :id LIMIT 1")
    suspend fun getDiscountById(id: Int): Discount?

    @Query("SELECT * FROM discount_table")
    suspend fun getAllDiscounts(): List<Discount>

    @Query("SELECT * FROM discount_table ORDER BY orderIndex ASC")
    suspend fun getAllDiscountsSorted(): List<Discount>

    @Update
    suspend fun update(discount: Discount)

    @Update
    suspend fun updateAll(discounts: List<Discount>)

    @Delete
    suspend fun delete(discount: Discount)

    @Query("DELETE FROM discount_table")
    suspend fun deleteAllDiscounts()
}