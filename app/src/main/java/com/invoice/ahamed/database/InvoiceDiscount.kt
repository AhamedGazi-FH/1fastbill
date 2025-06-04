package com.invoice.ahamed.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_discount_table")
data class InvoiceDiscount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val discountId: Int, // Links to the invoice
    val invoiceId: Int, // Links to the invoice
    val title: String,
    val percentage: Int,
    val price: Double,
    val isPlus: Boolean,
    val isActive: Boolean
)