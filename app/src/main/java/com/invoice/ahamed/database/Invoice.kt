package com.invoice.ahamed.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_table")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val invoiceId: Int = 0, // Primary key
    var name: String,
    var timestamp: Long,
    var total: Double,
    var discountData: String? = null // Optional field for serialized discounts
)