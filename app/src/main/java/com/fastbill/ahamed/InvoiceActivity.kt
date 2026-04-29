package com.fastbill.ahamed

import android.content.Intent
import android.os.Bundle
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InvoiceActivity : AppCompatActivity() {

    lateinit var binding: ActivityInvoiceBinding
    private lateinit var itemInvoiceAdapter: ItemInvoiceAdapter
    private lateinit var discountList: MutableList<Invoice>
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

    }

    private fun fetchAndDisplayDiscounts() {
        lifecycleScope.launch {
            discountList.clear()
            discountList.addAll(invoiceDao.getAllInvoices())
            itemInvoiceAdapter.notifyDataSetChanged()
        }
    }

    fun onPerformAction(position: Int, action: DiscountAction) {
        lifecycleScope.launch {
            val discount = discountList[position]
            when (action) {
                DiscountAction.DELETE -> {
                    invoiceDao.deleteInvoiceById(discount.invoiceId)
                    discountList.removeAt(position)
                    itemInvoiceAdapter.notifyItemRemoved(position)
                }
                DiscountAction.EDIT -> {
                }
                DiscountAction.ACTIVATE -> {
                    recreateActivityWithAnimation(position)
                }
            }
        }
    }

    private fun recreateActivityWithAnimation(position: Int) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("invoiceId", discountList[position].invoiceId)
        intent.putExtra("invoiceName", discountList[position].name)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        finish()
    }

}