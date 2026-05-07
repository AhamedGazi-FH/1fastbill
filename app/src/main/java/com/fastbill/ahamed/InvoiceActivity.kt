package com.fastbill.ahamed

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.fastbill.ahamed.database.Invoice
import com.fastbill.ahamed.database.InvoiceDatabase
import com.fastbill.ahamed.model.TopCustomer
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class InvoiceActivity : ComponentActivity() {

    private val database by lazy { InvoiceDatabase.getDatabase(this) }
    private val invoiceDao by lazy { database.invoiceDao() }
    private val itemDao by lazy { database.itemDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6750A4),
                    background = Color(0xFFF0F2F5),
                    surface = Color.White,
                    error = Color(0xFFD32F2F)
                )
            ) {
                HistoryDashboardScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    fun HistoryDashboardScreen() {
        val coroutineScope = rememberCoroutineScope()
        var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
        var showClearDialog by remember { mutableStateOf(false) }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val endOfMonth = calendar.timeInMillis

        val monthlyTotal by invoiceDao.getMonthlyTotal(startOfMonth, endOfMonth).collectAsState(initial = 0.0)
        val monthlyCount by invoiceDao.getMonthlyCount(startOfMonth, endOfMonth).collectAsState(initial = 0)
        val topCustomers by invoiceDao.getTopCustomers(startOfMonth, endOfMonth).collectAsState(initial = emptyList())

        LaunchedEffect(Unit) {
            invoices = invoiceDao.getAllInvoices()
        }

        val groupedInvoices = remember(invoices) {
            val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            invoices.groupBy { sdf.format(Date(it.timestamp)) }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Intelligence Dashboard", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (invoices.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Button(
                        onClick = { startActivity(Intent(this@InvoiceActivity, SharedInboxActivity::class.java)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Open Shared Inbox", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                item {
                    KPIDashboardCard(monthlyTotal = monthlyTotal, monthlyCount = monthlyCount, topCustomers = topCustomers)
                }

                item {
                    Text(
                        text = "INVOICE LEDGER",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                groupedInvoices.forEach { (date, dailyInvoices) ->
                    stickyHeader {
                        DateHeader(date = date, invoices = dailyInvoices)
                    }
                    items(dailyInvoices, key = { it.invoiceId }) { invoice ->
                        Box(modifier = Modifier.animateItem()) {
                            SwipeableInvoiceRow(
                                invoice = invoice,
                                onClick = {
                                    val intent = Intent(this@InvoiceActivity, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    intent.putExtra("invoiceId", invoice.invoiceId)
                                    intent.putExtra("invoiceName", invoice.name)
                                    startActivity(intent)
                                    finish()
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        invoiceDao.deleteInvoiceById(invoice.invoiceId)
                                        invoices = invoices.filter { it.invoiceId != invoice.invoiceId }
                                        Toast.makeText(this@InvoiceActivity, "Invoice Deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear History") },
                text = { Text("Are you sure you want to delete all invoice history? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            itemDao.deleteAllItems()
                            invoiceDao.deleteAllInvoices()
                            invoices = emptyList()
                            showClearDialog = false
                        }
                    }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
                }
            )
        }
    }

    @Composable
    fun KPIDashboardCard(monthlyTotal: Double?, monthlyCount: Int, topCustomers: List<TopCustomer>) {
        val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("This Month's Volume", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }

                Text(
                    text = "₹ ${indianFormat.format(monthlyTotal?.roundToInt() ?: 0)}",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Across $monthlyCount successful bills",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color(0xFFF0F0F0))

                Text("TOP BUYERS LEADERBOARD", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))

                if (topCustomers.isEmpty()) {
                    Text("No billing data yet this month.", fontSize = 14.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    topCustomers.forEachIndexed { index, customer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (index) {
                                            0 -> Color(0xFFFFD700).copy(alpha = 0.2f) // Gold
                                            1 -> Color(0xFFC0C0C0).copy(alpha = 0.2f) // Silver
                                            2 -> Color(0xFFCD7F32).copy(alpha = 0.2f) // Bronze
                                            else -> Color(0xFFF0F0F0)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (index) {
                                        0 -> Color(0xFFB8860B)
                                        1 -> Color(0xFF696969)
                                        2 -> Color(0xFF8B4513)
                                        else -> Color.Gray
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.customerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${customer.totalBills} Bills generated",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            Text(
                                text = "₹ ${indianFormat.format(customer.totalAmount.roundToInt())}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D32),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DateHeader(date: String, invoices: List<Invoice>) {
        val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))
        val dailyTotal = invoices.sumOf { it.total }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = date.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${invoices.size} Bills  •  ₹ ${indianFormat.format(dailyTotal.roundToInt())}",
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                fontSize = 13.sp
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SwipeableInvoiceRow(invoice: Invoice, onClick: () -> Unit, onDelete: () -> Unit) {
        val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))
        var showConfirmDialog by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.EndToStart) {
                    showConfirmDialog = true
                    false // Return false to snap back immediately, we will delete via dialog
                } else {
                    false
                }
            }
        )

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showConfirmDialog = false
                    scope.launch { dismissState.reset() }
                },
                title = { Text("Delete Bill") },
                text = { Text("Are you sure you want to delete this bill? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete()
                        showConfirmDialog = false
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                        scope.launch { dismissState.reset() }
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                val color by animateColorAsState(
                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                        MaterialTheme.colorScheme.error
                    else
                        Color.LightGray,
                    label = "swipeColor"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = invoice.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.Black
                        )
                        Text(
                            text = "Tap to edit or share",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Text(
                        text = "₹ ${indianFormat.format(invoice.total.roundToInt())}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
