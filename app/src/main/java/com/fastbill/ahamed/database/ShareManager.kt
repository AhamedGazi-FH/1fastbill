package com.fastbill.ahamed.database

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.fastbill.ahamed.model.SharedBillPackage
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class ShareManager(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()

    fun shareBillTemporarily(bill: Invoice, items: List<Item>, discounts: List<Discount>, senderName: String) {
        val sharedId = UUID.randomUUID().toString()
        val packageData = SharedBillPackage(
            sharedId = sharedId,
            senderName = senderName,
            timestamp = System.currentTimeMillis(),
            bill = bill,
            billItems = items,
            discounts = discounts // Passing the discounts
        )

        db.collection("temporary_bills")
            .document(sharedId)
            .set(packageData)
            .addOnSuccessListener {
                Toast.makeText(context, "Bill sent to shared inbox!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to share bill: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun openWhatsAppDirectChat(number: String, message: String) {
        val formattedNumber = number.replace("+", "").replace(" ", "")
        val url = "https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Try regular WhatsApp first
        intent.setPackage("com.whatsapp")
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Try WhatsApp Business
            intent.setPackage("com.whatsapp.w4b")
            try {
                context.startActivity(intent)
            } catch (e2: Exception) {
                // Fallback to any app that can handle the URI
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        }
    }
}
