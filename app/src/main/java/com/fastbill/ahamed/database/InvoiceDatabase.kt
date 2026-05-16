package com.fastbill.ahamed.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Invoice::class, Item::class, Discount::class, Rate::class, InvoiceDiscount::class, Customer::class, SyncLog::class],
    version = 9,
    exportSchema = false
)
abstract class InvoiceDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun itemDao(): ItemDao
    abstract fun discountDao(): DiscountDao
    abstract fun rateDao(): RateDao
    abstract fun invoiceDiscountDao(): InvoiceDiscountDao
    abstract fun customerDao(): CustomerDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: InvoiceDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE discount_table ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { /* Column exists */ }

                try {
                    database.execSQL("ALTER TABLE invoice_discount_table ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { /* Column exists */ }

                try {
                    database.execSQL("ALTER TABLE customer_table ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { /* Column exists */ }
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val currentTime = System.currentTimeMillis()
                try {
                    database.execSQL("ALTER TABLE rate_table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $currentTime")
                } catch (e: Exception) { /* Column might exist */ }
                
                try {
                    database.execSQL("ALTER TABLE rate_table ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { /* Column might exist */ }
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE rate_table ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { /* Column might exist */ }
            }
        }

        fun getDatabase(context: Context): InvoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "invoice_database"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}