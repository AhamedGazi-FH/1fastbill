package com.fastbill.ahamed.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "discount_table")
data class Discount(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var title: String = "",
    var percentage: Int = 0,
    var price: Double = 0.0,
    var isPlus: Boolean = false,
    var isActive: Boolean = false,
    var invoiceId: Int? = null,
    var orderIndex: Int = 0,
    @Ignore var amount: Double = 0.0 // Sequential absolute value for UI display
) {
    // Secondary constructor for Room to use (ignores '@Ignore' amount field)
    constructor(
        id: Int,
        title: String,
        percentage: Int,
        price: Double,
        isPlus: Boolean,
        isActive: Boolean,
        invoiceId: Int?,
        orderIndex: Int
    ) : this(id, title, percentage, price, isPlus, isActive, invoiceId, orderIndex, 0.0)
}
