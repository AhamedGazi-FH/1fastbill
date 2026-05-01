package com.fastbill.ahamed.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_table")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val invoiceId: Int = 0,
    var name: String = "",
    var timestamp: Long = 0L,
    var total: Double = 0.0,
    var discountData: String? = null
)
