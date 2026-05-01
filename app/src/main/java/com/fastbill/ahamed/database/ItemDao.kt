package com.fastbill.ahamed.database

import androidx.room.*

@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Item>)

    @Query("SELECT * FROM item_table WHERE invoiceId = :invoiceId")
    suspend fun getItemsForInvoice(invoiceId: Int): List<Item>

    @Query("SELECT * FROM item_table")
    suspend fun getAllItems(): List<Item>

    @Query("DELETE FROM item_table WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Int)

    @Query("DELETE FROM item_table")
    suspend fun deleteAllItems()
}