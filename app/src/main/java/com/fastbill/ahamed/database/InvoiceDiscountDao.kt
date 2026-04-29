package com.fastbill.ahamed.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InvoiceDiscountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoiceDiscount: InvoiceDiscount)

    @Query("DELETE FROM invoice_discount_table WHERE invoiceId = :invoiceId")
    suspend fun deleteByInvoiceId(invoiceId: Int)

    @Query("SELECT * FROM invoice_discount_table WHERE invoiceId = :invoiceId")
    suspend fun getDiscountsForInvoice(invoiceId: Int): List<InvoiceDiscount>
}