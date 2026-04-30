package com.fastbill.ahamed.database

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object CSVImporter {

    suspend fun importCustomersFromCSV(context: Context, uri: Uri, customerDao: CustomerDao): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val customers = mutableListOf<Customer>()
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            // Skip header
            val header = reader.readLine()
            
            var line: String? = reader.readLine()
            while (line != null) {
                val tokens = line.split(",")
                if (tokens.isNotEmpty()) {
                    val name = tokens[0].trim().replace("\"", "")
                    val phone = if (tokens.size > 1) tokens[1].trim().replace("\"", "") else null
                    
                    if (name.isNotEmpty()) {
                        customers.add(Customer(customerName = name, phoneNumber = phone))
                    }
                }
                line = reader.readLine()
            }
            reader.close()
            
            if (customers.isNotEmpty()) {
                customerDao.insertMultipleCustomers(customers)
            }
            Result.success(customers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
