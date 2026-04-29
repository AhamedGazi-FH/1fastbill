package com.fastbill.ahamed.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Invoice::class, Item::class, Discount::class, Rate::class, InvoiceDiscount::class],
    version = 4,
    exportSchema = false
)
abstract class InvoiceDatabase : RoomDatabase() {

    abstract fun invoiceDao(): InvoiceDao
    abstract fun itemDao(): ItemDao
    abstract fun discountDao(): DiscountDao
    abstract fun rateDao(): RateDao
    abstract fun invoiceDiscountDao(): InvoiceDiscountDao

    companion object {
        @Volatile
        private var INSTANCE: InvoiceDatabase? = null

        fun getDatabase(context: Context): InvoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "invoice_database"
                )
                    .fallbackToDestructiveMigration() // Use this if you want to reset the database on schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}