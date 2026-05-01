package com.fastbill.ahamed

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fastbill.ahamed.databinding.ActivitySharedInboxBinding
import com.fastbill.ahamed.databinding.ItemSharedBillBinding
import com.fastbill.ahamed.model.SharedBillPackage
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class SharedInboxActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySharedInboxBinding
    private val db = FirebaseFirestore.getInstance()
    private val sharedBills = mutableListOf<SharedBillPackage>()
    private lateinit var adapter: SharedInboxAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySharedInboxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imgBack.setOnClickListener { finish() }

        adapter = SharedInboxAdapter(sharedBills) { billPackage ->
            adoptBill(billPackage)
        }
        binding.rvSharedInbox.layoutManager = LinearLayoutManager(this)
        binding.rvSharedInbox.adapter = adapter

        setupSwipeToDelete()

        binding.btnClearAll.setOnClickListener {
            showClearAllConfirmation()
        }

        fetchSharedBills()
    }

    private fun fetchSharedBills() {
        db.collection("temporary_bills")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                sharedBills.clear()
                for (document in result) {
                    val billPackage = document.toObject(SharedBillPackage::class.java)
                    sharedBills.add(billPackage)
                }
                adapter.notifyDataSetChanged()
                updateClearButtonVisibility()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch bills: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateClearButtonVisibility() {
        binding.btnClearAll.visibility = if (sharedBills.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val itemToDelete = sharedBills[position]

                sharedBills.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateClearButtonVisibility()

                db.collection("temporary_bills").document(itemToDelete.sharedId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this@SharedInboxActivity, "Removed from Inbox", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvSharedInbox)
    }

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear Inbox")
            .setMessage("Clear all temporary bills?")
            .setPositiveButton("Clear All") { _, _ ->
                clearAllBillsFromFirestore()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllBillsFromFirestore() {
        val batch = db.batch()
        sharedBills.forEach { bill ->
            val docRef = db.collection("temporary_bills").document(bill.sharedId)
            batch.delete(docRef)
        }

        batch.commit().addOnSuccessListener {
            sharedBills.clear()
            adapter.notifyDataSetChanged()
            updateClearButtonVisibility()
            Toast.makeText(this, "Inbox cleared", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to clear inbox: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun adoptBill(billPackage: SharedBillPackage) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("invoiceId", -1)
        intent.putExtra("shared_customer_name", billPackage.bill.name)
        SharedDataHolder.itemsToAdopt = billPackage.billItems
        startActivity(intent)
        finish()
    }
}

class SharedInboxAdapter(
    private val bills: List<SharedBillPackage>,
    private val onClick: (SharedBillPackage) -> Unit
) : RecyclerView.Adapter<SharedInboxAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSharedBillBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSharedBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = bills[position]
        val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

        holder.binding.tvCustomerName.text = item.bill.name
        holder.binding.tvTotalAmount.text = "₹ ${indianFormat.format(item.bill.total)}"
        holder.binding.tvSenderName.text = "Sent by: ${item.senderName}"
        holder.binding.tvItemCount.text = "${item.billItems.size} Items"

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = bills.size
}

object SharedDataHolder {
    var itemsToAdopt: List<com.fastbill.ahamed.database.Item>? = null
}
