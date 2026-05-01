package com.fastbill.ahamed.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discount_table")
data class Discount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String = "",
    var percentage: Int = 0,
    var price: Double = 0.0,
    var isPlus: Boolean = false,
    var isActive: Boolean = false,
    var invoiceId: Int? = null,
    var orderIndex: Int = 0
)
