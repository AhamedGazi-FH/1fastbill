package com.fastbill.ahamed

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fastbill.ahamed.adapter.ItemInvoiceAdapter
import com.fastbill.ahamed.database.Invoice
import com.fastbill.ahamed.database.InvoiceDatabase
import com.fastbill.ahamed.databinding.ActivityInvoiceBinding
import com.fastbill.ahamed.model.DiscountAction
import com.fastbill.ahamed.model.HistoryListItem
import com.fastbill.ahamed.model.TopCustomer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class InvoiceActivity : AppCompatActivity() {

    lateinit var binding: ActivityInvoiceBinding
    private lateinit var itemInvoiceAdapter: ItemInvoiceAdapter
    private lateinit var discountList: MutableList<HistoryListItem>
    private val database by lazy {
        InvoiceDatabase.getDatabase(this)
    }
    private val invoiceDao by lazy {
        database.invoiceDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.imgBack.setOnClickListener {
            finish()
        }
        discountList = mutableListOf()
        itemInvoiceAdapter = ItemInvoiceAdapter(discountList, ::onPerformAction)
        binding.rvInvoice.layoutManager = LinearLayoutManager(this)
        binding.rvInvoice.adapter = itemInvoiceAdapter
        fetchAndDisplayDiscounts()
        observeMonthlyKPIs()
    }

    private fun observeMonthlyKPIs() {
        val calendar = Calendar.getInstance()
        
        // Start of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        // End of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis

        val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

        lifecycleScope.launch {
            invoiceDao.getMonthlyTotal(startOfMonth, endOfMonth).collectLatest { total ->
                val formattedTotal = total?.roundToInt() ?: 0
                binding.tvMonthlyTotal.text = "₹ ${indianFormat.format(formattedTotal)}"
            }
        }

        lifecycleScope.launch {
            invoiceDao.getMonthlyCount(startOfMonth, endOfMonth).collectLatest { count ->
                binding.tvMonthlyCount.text = "Total Bills: $count"
            }
        }

        lifecycleScope.launch {
            invoiceDao.getTopCustomers(startOfMonth, endOfMonth).collectLatest { customers ->
                updateTopCustomersUI(customers)
            }
        }
    }

    private fun updateTopCustomersUI(customers: List<TopCustomer>) {
        binding.llTopCustomers.removeAllViews()
        if (customers.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No bills this month"
                textSize = 14f
                setTextColor(Color.GRAY)
                setPadding(0, 8, 0, 0)
            }
            binding.llTopCustomers.addView(emptyTv)
            return
        }

        val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

        customers.forEach { customer ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 12, 0, 12)
                weightSum = 10f
            }

            val nameTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 5f)
                text = customer.customerName
                textSize = 14f
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
            }

            val countTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                text = "${customer.totalBills} Bills"
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(Color.DKGRAY)
            }

            val amountTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
                text = "₹ ${indianFormat.format(customer.totalAmount.roundToInt())}"
                textSize = 14f
                gravity = Gravity.END
                setTextColor(Color.parseColor("#2E7D32")) // Success green
                typeface = Typeface.DEFAULT_BOLD
            }

            row.addView(nameTv)
            row.addView(countTv)
            row.addView(amountTv)
            binding.llTopCustomers.addView(row)
        }
    }

    private fun fetchAndDisplayDiscounts() {
        lifecycleScope.launch {
            val invoices = invoiceDao.getAllInvoices()
            val groupedList = groupInvoicesByDate(invoices)
            discountList.clear()
            discountList.addAll(groupedList)
            itemInvoiceAdapter.notifyDataSetChanged()
        }
    }

    private fun groupInvoicesByDate(invoices: List<Invoice>): List<HistoryListItem> {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val grouped = invoices.groupBy { sdf.format(Date(it.timestamp)) }
        val result = mutableListOf<HistoryListItem>()

        grouped.forEach { (date, invoicesInDate) ->
            val dailyTotal = invoicesInDate.sumOf { it.total }
            result.add(HistoryListItem.DateHeader(date, invoicesInDate.size, dailyTotal))
            invoicesInDate.forEach { invoice ->
                result.add(HistoryListItem.BillData(invoice))
            }
        }
        return result
    }

    fun onPerformAction(position: Int, action: DiscountAction) {
        lifecycleScope.launch {
            val item = discountList[position]
            if (item is HistoryListItem.BillData) {
                val invoice = item.invoice
                when (action) {
                    DiscountAction.DELETE -> {
                        invoiceDao.deleteInvoiceById(invoice.invoiceId)
                        fetchAndDisplayDiscounts()
                    }
                    DiscountAction.EDIT -> {
                    }
                    DiscountAction.ACTIVATE -> {
                        val intent = Intent(this@InvoiceActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.putExtra("invoiceId", invoice.invoiceId)
                        intent.putExtra("invoiceName", invoice.name)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
                        finish()
                    }
                }
            }
        }
    }
}