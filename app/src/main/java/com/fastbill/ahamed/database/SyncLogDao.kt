package com.fastbill.ahamed.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SyncLogDao {
    @Insert
    suspend fun insert(log: SyncLog)

    @Query("SELECT * FROM sync_log_table ORDER BY timestamp DESC LIMIT 20")
    suspend fun getLastLogs(): List<SyncLog>
}
