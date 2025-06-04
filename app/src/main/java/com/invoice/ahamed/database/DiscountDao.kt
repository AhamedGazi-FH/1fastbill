package com.invoice.ahamed.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DiscountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discount: Discount)

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
}