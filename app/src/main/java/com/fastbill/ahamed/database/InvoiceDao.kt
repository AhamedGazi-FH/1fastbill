package com.fastbill.ahamed.database

import androidx.room.*
import com.fastbill.ahamed.model.TemporaryItem

@Dao
interface InvoiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: Invoice): Long // Returns the auto-generated invoiceId

    @Query("SELECT * FROM invoice_table WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceById(invoiceId: Int): Invoice?

    @Query("SELECT * FROM invoice_table ORDER BY timestamp DESC")
    suspend fun getAllInvoices(): List<Invoice>

    @Query("DELETE FROM invoice_table WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceById(invoiceId: Int)

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    suspend fun convertToDatabaseItems(
        temporaryItems: List<TemporaryItem>,
        invoiceId: Int
    ): List<Item> {
        return temporaryItems.map { temporaryItem ->
            Item(
                invoiceId = invoiceId,
                name = temporaryItem.name,
                quantity = temporaryItem.quantity,
                rate = temporaryItem.rate,
                total = temporaryItem.total
            )
        }
    }
}