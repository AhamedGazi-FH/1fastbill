package com.fastbill.ahamed.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Invoice::class, Item::class, Discount::class, Rate::class, InvoiceDiscount::class, Customer::class, SyncLog::class],
    version = 7, // INCREMENTED: We bumped this from 6 to 7
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

        // THE LIFESAVER: This teaches Android how to upgrade old databases without deleting data.
        // We use try-catch blocks so it works perfectly even if the user already cleared their data.
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

        fun getDatabase(context: Context): InvoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "invoice_database"
                )
                    // DELETED: .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_6_7) // ADDED: Safe Migration
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}