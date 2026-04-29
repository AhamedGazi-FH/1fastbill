package com.fastbill.ahamed.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discount_table")
data class Discount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String,
    var percentage: Int,
    var price: Double,
    var isPlus: Boolean,
    var isActive: Boolean,
    var invoiceId: Int? = null, // Nullable field to associate with an invoice
    var orderIndex: Int = 0
)