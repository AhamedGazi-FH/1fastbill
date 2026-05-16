package com.fastbill.ahamed.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rate_table")
data class Rate(
    @PrimaryKey val item_name: String,
    var rate: Double,
    var updatedAt: Long = System.currentTimeMillis(),
    var isSynced: Boolean = false,
    var isDeleted: Boolean = false
)