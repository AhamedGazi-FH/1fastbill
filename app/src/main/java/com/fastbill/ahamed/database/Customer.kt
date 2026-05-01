package com.fastbill.ahamed.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_table",
    indices = [Index(value = ["customerName"], unique = true)]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val phoneNumber: String? = null,
    val isSynced: Boolean = false,
    val firestoreId: String = "",
    val isDeleted: Boolean = false
)
