package com.fastbill.ahamed.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_log_table")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val status: String,
    val details: String
)
