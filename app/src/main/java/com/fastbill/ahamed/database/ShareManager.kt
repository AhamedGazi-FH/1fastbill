package com.fastbill.ahamed.database

import android.content.Context
import android.widget.Toast
import com.fastbill.ahamed.model.SharedBillPackage
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ShareManager(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()

    fun shareBillTemporarily(bill: Invoice, items: List<Item>, senderName: String) {
        val sharedId = UUID.randomUUID().toString()
        val packageData = SharedBillPackage(
            sharedId = sharedId,
            senderName = senderName,
            timestamp = System.currentTimeMillis(),
            bill = bill,
            billItems = items
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
}
