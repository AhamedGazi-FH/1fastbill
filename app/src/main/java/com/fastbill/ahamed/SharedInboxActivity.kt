package com.fastbill.ahamed

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.fastbill.ahamed.model.SharedBillPackage
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

// UI Model strictly mapped to your SharedBillPackage
data class SharedInvoiceItem(
    val id: String,
    val senderName: String,
    val customerName: String,
    val amount: Double,
    val itemCount: Int,
    var isRead: Boolean = false,
    val rawPackage: SharedBillPackage
)

class SharedInboxActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF6750A4), background = Color(0xFFF2F4F7))) {

                var inboxItems by remember { mutableStateOf<List<SharedInvoiceItem>>(emptyList()) }
                var isRefreshing by remember { mutableStateOf(false) }
                var showClearDialog by remember { mutableStateOf(false) }

                // --- EXACT FIREBASE LOGIC FROM YOUR OLD XML ---
                fun fetchSharedBills() {
                    isRefreshing = true
                    db.collection("temporary_bills")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { result ->
                            val newItems = mutableListOf<SharedInvoiceItem>()
                            for (document in result) {
                                try {
                                    val pkg = document.toObject(SharedBillPackage::class.java)
                                    newItems.add(
                                        SharedInvoiceItem(
                                            id = pkg.sharedId,
                                            senderName = pkg.senderName ?: "Unknown Sender",
                                            customerName = pkg.bill.name ?: "Unknown Customer",
                                            amount = pkg.bill.total,
                                            itemCount = pkg.billItems.size,
                                            isRead = false,
                                            rawPackage = pkg
                                        )
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("FirebaseSync", "Skipped corrupted bill ${document.id}: ${e.message}")
                                }
                            }
                            inboxItems = newItems
                            isRefreshing = false
                        }
                        .addOnFailureListener { e ->
                            isRefreshing = false
                            Toast.makeText(this, "Failed to fetch bills: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                // Initial fetch
                LaunchedEffect(Unit) {
                    fetchSharedBills()
                }

                // Adopt Bill Logic (Exact match to your intent extras)
                fun adoptBill(item: SharedInvoiceItem) {
                    val intent = Intent(this@SharedInboxActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("invoiceId", -1)
                    intent.putExtra("shared_customer_name", item.customerName)

                    com.fastbill.ahamed.model.SharedDataHolder.itemsToAdopt = item.rawPackage.billItems
                    com.fastbill.ahamed.model.SharedDataHolder.discountsToAdopt = item.rawPackage.discounts

                    startActivity(intent)
                    finish()
                }

                // Delete Single Bill Logic
                fun deleteBill(item: SharedInvoiceItem) {
                    inboxItems = inboxItems.filter { it.id != item.id }
                    db.collection("temporary_bills").document(item.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this@SharedInboxActivity, "Removed from Inbox", Toast.LENGTH_SHORT).show()
                        }
                }

                // Clear All Logic
                fun clearAllBills() {
                    val batch = db.batch()
                    inboxItems.forEach { bill ->
                        val docRef = db.collection("temporary_bills").document(bill.id)
                        batch.delete(docRef)
                    }
                    batch.commit().addOnSuccessListener {
                        inboxItems = emptyList()
                        Toast.makeText(this@SharedInboxActivity, "Inbox cleared", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this@SharedInboxActivity, "Failed to clear inbox: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("Shared Inbox", fontWeight = FontWeight.Black, fontSize = 20.sp)
                                    if (inboxItems.isNotEmpty()) {
                                        Text("${inboxItems.size} Unread Bills", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Default.ArrowBack, null) } },
                            actions = {
                                if (inboxItems.isNotEmpty()) {
                                    IconButton(onClick = { showClearDialog = true }) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }

                                val rotation by animateFloatAsState(
                                    targetValue = if (isRefreshing) 360f else 0f,
                                    animationSpec = if (isRefreshing) {
                                        infiniteRepeatable(animation = tween<Float>(durationMillis = 1000, easing = LinearEasing), repeatMode = RepeatMode.Restart)
                                    } else {
                                        tween<Float>(durationMillis = 300)
                                    },
                                    label = "RefreshRotation"
                                )
                                IconButton(onClick = {
                                    fetchSharedBills()
                                }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.rotate(rotation)) }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF2F4F7))) {

                        if (inboxItems.isEmpty() && !isRefreshing) {
                            EmptyInboxView()
                        } else {
                            LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp), modifier = Modifier.fillMaxSize()) {
                                items(inboxItems, key = { it.id }) { invoice ->
                                    Box(modifier = Modifier.animateItem()) {
                                        SwipeableInboxRow(
                                            invoice = invoice,
                                            onEdit = { adoptBill(invoice) },
                                            onReject = { deleteBill(invoice) },
                                            onClick = { adoptBill(invoice) }
                                        )
                                    }
                                }
                            }
                        }

                        if (isRefreshing && inboxItems.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Clear All Confirmation Dialog
                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        title = { Text("Clear Inbox", fontWeight = FontWeight.Bold) },
                        text = { Text("Are you sure you want to clear all temporary bills? This cannot be undone.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showClearDialog = false
                                clearAllBills()
                            }) { Text("Clear All", color = Color.Red, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SwipeableInboxRow(invoice: SharedInvoiceItem, onEdit: () -> Unit, onReject: () -> Unit, onClick: () -> Unit) {
        val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> { onEdit(); false }
                    SwipeToDismissBoxValue.EndToStart -> { onReject(); true }
                    else -> false
                }
            },
            // 🚀 EXPERT FIX: Require 50% deliberate swipe distance to ignore diagonal scrolling
            positionalThreshold = { totalDistance -> totalDistance * 0.5f }
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val color by animateColorAsState(
                    targetValue = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2196F3)
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935)
                        else -> Color.Transparent
                    },
                    label = "SwipeBackgroundColor"
                )

                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(16.dp)).background(color).padding(horizontal = 24.dp),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(28.dp))
                            Text(" IMPORT", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        }
                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("DELETE ", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { onClick() },
                colors = CardDefaults.cardColors(containerColor = if (invoice.isRead) Color(0xFFF9FAFB) else Color.White),
                elevation = CardDefaults.cardElevation(if (invoice.isRead) 1.dp else 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = invoice.customerName.take(1).uppercase(),
                            fontWeight = FontWeight.Black, fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = invoice.customerName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp, color = Color.Black,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "₹ ${indianFormat.format(invoice.amount.roundToInt())}",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Sent by: ${invoice.senderName}", fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = "${invoice.itemCount} Items", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun EmptyInboxView() {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape).background(Color(0xFFE8EAF6)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Inbox, contentDescription = null, tint = Color(0xFF9FA8DA), modifier = Modifier.size(60.dp)) }
            Spacer(Modifier.height(24.dp))
            Text("Inbox Empty", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF374151))
            Spacer(Modifier.height(8.dp))
            Text("There are no new shared invoices waiting for you.", fontSize = 14.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
