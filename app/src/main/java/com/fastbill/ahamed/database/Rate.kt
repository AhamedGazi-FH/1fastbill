package com.fastbill.ahamed.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rate_table")
data class Rate(
    @PrimaryKey val item_name: String, // Primary key (unique item name)
    var rate: Double                   // Rate for the item
)