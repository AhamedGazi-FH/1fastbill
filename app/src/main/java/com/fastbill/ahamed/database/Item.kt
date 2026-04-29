package com.fastbill.ahamed.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_table",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["invoiceId"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE // Delete items when the associated invoice is deleted
        )
    ]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val itemId: Int = 0,
    val invoiceId: Int, // Foreign key linking to the Invoice table
    val name: String,
    val quantity: Int,
    val rate: Double,
    val total: Double
)