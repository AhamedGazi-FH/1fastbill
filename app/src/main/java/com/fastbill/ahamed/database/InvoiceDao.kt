package com.fastbill.ahamed.database

import androidx.room.*
import com.fastbill.ahamed.model.TemporaryItem
import com.fastbill.ahamed.model.TopCustomer
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: Invoice): Long // Returns the auto-generated invoiceId

    @Query("SELECT * FROM invoice_table WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceById(invoiceId: Int): Invoice?

    @Query("SELECT * FROM invoice_table ORDER BY timestamp DESC")
    suspend fun getAllInvoices(): List<Invoice>

    @Query("SELECT SUM(total) FROM invoice_table WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    fun getDailyTotal(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(total) FROM invoice_table WHERE timestamp >= :startOfMonth AND timestamp <= :endOfMonth")
    fun getMonthlyTotal(startOfMonth: Long, endOfMonth: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM invoice_table WHERE timestamp >= :startOfMonth AND timestamp <= :endOfMonth")
    fun getMonthlyCount(startOfMonth: Long, endOfMonth: Long): Flow<Int>

    @Query("SELECT name as customerName, COUNT(*) as totalBills, SUM(total) as totalAmount FROM invoice_table WHERE timestamp >= :startOfMonth AND timestamp <= :endOfMonth GROUP BY name ORDER BY totalAmount DESC LIMIT 5")
    fun getTopCustomers(startOfMonth: Long, endOfMonth: Long): Flow<List<TopCustomer>>

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