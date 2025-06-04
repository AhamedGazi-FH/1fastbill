package com.invoice.ahamed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.invoice.ahamed.adapter.ItemAdapter
import com.invoice.ahamed.adapter.ItemDiscountAdapter
import com.invoice.ahamed.database.Discount
import com.invoice.ahamed.database.Invoice
import com.invoice.ahamed.database.InvoiceDatabase
import com.invoice.ahamed.database.InvoiceDiscount
import com.invoice.ahamed.database.Rate
import com.invoice.ahamed.databinding.ActivityMainBinding
import com.invoice.ahamed.databinding.EditDiscountDialogBinding
import com.invoice.ahamed.model.DiscountAction
import com.invoice.ahamed.model.TemporaryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    private lateinit var itemList: MutableList<TemporaryItem>
    private lateinit var adapter: ItemAdapter
    private lateinit var discountAdapter: ItemDiscountAdapter
    lateinit var binding: ActivityMainBinding
    private lateinit var rateDatabase: InvoiceDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var discountList: MutableList<Discount>
    private var selectedColorCode = "#6750A4"
    private val database by lazy {
        InvoiceDatabase.getDatabase(this)
    }
    private val discountDao by lazy {
        database.discountDao()
    }
    private val invoiceDao by lazy {
        database.invoiceDao()
    }
    private val itemDao by lazy {
        database.itemDao()
    }
    private val invoiceDiscountDao by lazy {
        database.invoiceDiscountDao()
    }

    private var invoiceId = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isActivated = sharedPreferences.getBoolean("isActivated", false)
        if (isActivated) {
            binding.activate.relActivate.visibility = View.GONE
        } else {
            binding.activate.relActivate.visibility = View.VISIBLE
            binding.activate.etCode.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.activate.btnActivate.performClick()
                    true // Consume the event
                } else {
                    false // Let the system handle other actions
                }
            }
            binding.activate.btnActivate.setOnClickListener {
                val code = binding.activate.etCode.text.toString().toLongOrNull() ?: 0
                if (code > 0 && (code == Utils.ACTIVATION_CODE)) {
                    binding.activate.relActivate.visibility = View.GONE
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isActivated", true)
                    editor.apply()
                    val inputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(
                        binding.activate.etCode.windowToken, 0
                    )
                } else {
                    Toast.makeText(this, "Please enter valid code", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.imgReset.setOnClickListener {
            recreateActivityWithAnimation()
        }

        binding.imgInvoice.setOnClickListener {
            startActivity(Intent(this, InvoiceActivity::class.java))
        }

        binding.imgSetting.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            intent.putExtra("invoiceId", invoiceId) // Pass the current invoiceId
            startActivity(intent)
        }

        rateDatabase = InvoiceDatabase.getDatabase(this)

        // Initialize item list and adapter
        itemList = mutableListOf()
        adapter = ItemAdapter(itemList, ::updateSummary)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        discountList = mutableListOf()
        discountAdapter =
            ItemDiscountAdapter(discountList, ::onPerformAction, itemList.sumOf { it.total })
        binding.rvDiscount.layoutManager = LinearLayoutManager(this)
        binding.rvDiscount.adapter = discountAdapter

        val itemTouchHelperCallback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // Enable drag only (no swipe)
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                return makeMovementFlags(dragFlags, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                // Swap items in the adapter
                discountAdapter.swapItems(fromPosition, toPosition)
                lifecycleScope.launch {
                    discountList.forEachIndexed { index, discount ->
                        discount.orderIndex = index
                        discountDao.update(discount)
                        updateSummary()
                    }
//                    fetchAndDisplayDiscounts()
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe functionality
            }

            override fun isLongPressDragEnabled(): Boolean {
                // Enable long-press drag
                return true
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvDiscount)


        // Initially hide the "Add to List" button
        binding.addItemButton.visibility = View.GONE
        binding.indexTextView.text = "${itemList.size + 1}."
        // Add item button click listener
        binding.addItemButton.setOnClickListener {
            val name = binding.itemNameInput.text.toString().trim()
            val quantity = binding.quantityInput.text.toString().toIntOrNull() ?: 0
            val rate = binding.rateInput.text.toString().toDoubleOrNull() ?: 0.00
            val total = quantity * rate

            if (name.isNotEmpty() && quantity > 0 && rate > 0) {
                val newItem = TemporaryItem(name, quantity, rate, total)
                itemList.add(newItem)
                adapter.notifyItemInserted(itemList.size - 1)
                updateSummary()
                clearInputs(binding.itemNameInput, binding.rateInput)
                binding.indexTextView.text = "${itemList.size + 1}."

                lifecycleScope.launch {
                    val existingRate = rateDatabase.rateDao().getRateByItemName(name)
                    if (existingRate != null) {
                        existingRate.rate = rate
                        rateDatabase.rateDao().update(existingRate)
                    } else {
                        val newRate = Rate(item_name = name, rate = rate)
                        rateDatabase.rateDao().insert(newRate)
                    }
                }
            } else {
                Toast.makeText(this, "Please enter valid values", Toast.LENGTH_SHORT).show()
            }
        }

        // Save button in toolbar
        binding.btnSave.setOnClickListener {
            val name = binding.edtUsername.text.toString().trim()
            val total = binding.tvFinalTotal.text.toString().trim().toDoubleOrNull() ?: 0.00
            if (name.isNotEmpty()) {
                if (itemList.size > 0) {
                    val invoice =
                        Invoice(name = name, timestamp = System.currentTimeMillis(), total = total)
                    lifecycleScope.launch {
                        if (invoiceId == -1) {
                            val invoiceId = invoiceDao.insert(invoice).toInt()
                            // Save active discounts for the invoice
                            val activeDiscounts = discountList.filter { it.isActive }
                            activeDiscounts.forEach { discount ->
                                val invoiceDiscount = InvoiceDiscount(
                                    invoiceId = invoiceId,
                                    discountId = discount.id,
                                    title = discount.title,
                                    percentage = discount.percentage,
                                    price = discount.price,
                                    isPlus = discount.isPlus,
                                    isActive = discount.isActive
                                )
                                invoiceDiscountDao.insert(invoiceDiscount)
                            }
                            itemDao.insertAll(
                                invoiceDao.convertToDatabaseItems(
                                    itemList, invoiceId
                                )
                            )
                            Toast.makeText(
                                this@MainActivity, "Record added successfully", Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            invoiceDao.getInvoiceById(invoiceId)?.let {
                                it.name = name
                                it.total = total
                                invoiceDao.updateInvoice(it)
                                // Clear old discounts and save new active discounts
                                invoiceDiscountDao.deleteByInvoiceId(it.invoiceId)
                                val activeDiscounts =
                                    discountList.filter { discount -> discount.isActive }
                                activeDiscounts.forEach { discount ->
                                    val invoiceDiscount = InvoiceDiscount(
                                        invoiceId = invoiceId,
                                        discountId = discount.id,
                                        title = discount.title,
                                        percentage = discount.percentage,
                                        price = discount.price,
                                        isPlus = discount.isPlus,
                                        isActive = discount.isActive
                                    )
                                    invoiceDiscountDao.insert(invoiceDiscount)
                                }
                                itemDao.deleteItemsForInvoice(it.invoiceId)
                                itemDao.insertAll(
                                    invoiceDao.convertToDatabaseItems(
                                        itemList, it.invoiceId
                                    )
                                )
                                Toast.makeText(
                                    this@MainActivity,
                                    "Record update successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        recreateActivityWithAnimation()
                    }
                } else Toast.makeText(this, "Please add at list one record", Toast.LENGTH_SHORT)
                    .show()
            } else Toast.makeText(this, "Please enter user name", Toast.LENGTH_SHORT).show()
        }

        binding.btnShare.setOnClickListener {
            val name = binding.edtUsername.text.toString().trim()
            if (name.isNotEmpty()) {
                if (itemList.size > 0) {
                    binding.tvName.setText(name)
                    binding.loading.loading.visibility = View.VISIBLE
                    binding.loading.loading.playAnimation()
                    binding.relUser.visibility = View.VISIBLE

                    lifecycleScope.launch {
                        val bitmap = getBitmapFromView(binding.llCapture)
                        Log.e("check_bitmap", "getting")
                        val savedUri = saveBitmapToCache(bitmap)
                        shareImage(savedUri)
                        withContext(Dispatchers.Main) {
                            binding.relUser.visibility = View.GONE
                            binding.loading.loading.pauseAnimation()
                            binding.loading.loading.visibility = View.GONE
                        }
                    }
                    val total = binding.tvFinalTotal.text.toString().trim().toDoubleOrNull() ?: 0.0
                    val invoice =
                        Invoice(name = name, timestamp = System.currentTimeMillis(), total = total)
                    lifecycleScope.launch {
                        if (invoiceId == -1) {
                            val invoiceId = invoiceDao.insert(invoice).toInt()
                            // Save active discounts for the invoice
                            val activeDiscounts = discountList.filter { it.isActive }
                            activeDiscounts.forEach { discount ->
                                val invoiceDiscount = InvoiceDiscount(
                                    invoiceId = invoiceId,
                                    discountId = discount.id,
                                    title = discount.title,
                                    percentage = discount.percentage,
                                    price = discount.price,
                                    isPlus = discount.isPlus,
                                    isActive = discount.isActive
                                )
                                invoiceDiscountDao.insert(invoiceDiscount)
                            }

                            itemDao.deleteItemsForInvoice(invoiceId)
                            itemDao.insertAll(
                                invoiceDao.convertToDatabaseItems(
                                    itemList, invoiceId
                                )
                            )
                            Toast.makeText(
                                this@MainActivity, "Record added successfully", Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            invoiceDao.getInvoiceById(invoiceId)?.let {
                                it.name = name
                                it.total = total
                                invoiceDao.updateInvoice(it)
                                // Clear old discounts and save new active discounts
                                invoiceDiscountDao.deleteByInvoiceId(it.invoiceId)
                                val activeDiscounts =
                                    discountList.filter { discount -> discount.isActive }
                                activeDiscounts.forEach { discount ->
                                    val invoiceDiscount = InvoiceDiscount(
                                        invoiceId = invoiceId,
                                        discountId = discount.id,
                                        title = discount.title,
                                        percentage = discount.percentage,
                                        price = discount.price,
                                        isPlus = discount.isPlus,
                                        isActive = discount.isActive
                                    )
                                    invoiceDiscountDao.insert(invoiceDiscount)
                                }
                                itemDao.deleteItemsForInvoice(it.invoiceId)
                                itemDao.insertAll(
                                    invoiceDao.convertToDatabaseItems(
                                        itemList, it.invoiceId
                                    )
                                )
                                Toast.makeText(
                                    this@MainActivity,
                                    "Record update successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        recreateActivityWithAnimation()
                    }
                } else Toast.makeText(this, "Please add at list one record", Toast.LENGTH_SHORT)
                    .show()
            } else Toast.makeText(this, "Please enter user name", Toast.LENGTH_SHORT).show()
        }

        addTextWatchers()

        if (intent.hasExtra("invoiceId")) {
            invoiceId = intent.getIntExtra("invoiceId", -1)
            if (invoiceId != -1) {
                binding.btnSave.setText(getString(R.string.update))

            }
            Log.e("invoiceId", "invoiceId: $invoiceId")
            lifecycleScope.launch {
                invoiceDao.getInvoiceById(invoiceId)?.let {
                    binding.edtUsername.setText(it.name)
                    binding.tvName.setText(it.name)
                    val date = convertTimestampToDate(it.timestamp)
                    binding.tvDate.text = date
                    binding.tvPrintDate.text = date
                }
                val list = itemDao.getItemsForInvoice(invoiceId)
                val temporaryItemList = list.map { item ->
                    TemporaryItem(
                        name = item.name,
                        quantity = item.quantity,
                        rate = item.rate,
                        total = item.total
                    )
                }
                itemList.addAll(temporaryItemList)
                adapter.notifyDataSetChanged()

                // Fetch all discounts and linked discounts
                val allDiscounts = discountDao.getAllDiscounts()
                val linkedDiscounts = invoiceDiscountDao.getDiscountsForInvoice(invoiceId)

                // Create a map for quick lookup of linked discounts by discountId
                val linkedDiscountMap = linkedDiscounts.associateBy { it.discountId }

                // Prepare a list of updated discounts to persist in the database
                val updatedDiscounts = mutableListOf<Discount>()

                allDiscounts.forEach { discount ->
                    val linkedDiscount = linkedDiscountMap[discount.id]
                    if (linkedDiscount != null) {
                        // Update discount with data from linkedDiscount
                        discount.isActive = true
                        discount.title = linkedDiscount.title
                        discount.percentage = linkedDiscount.percentage
                        discount.price = linkedDiscount.price
                        discount.isPlus = linkedDiscount.isPlus
                    } else {
                        // Deactivate the discount if not linked
                        discount.isActive = false
                    }
                    updatedDiscounts.add(discount)
                }

                // Batch update all discounts in the database
                discountDao.updateAll(updatedDiscounts)

                fetchAndDisplayDiscounts()
                updateSummary()
            }
        } else {
            val date = convertTimestampToDate(System.currentTimeMillis())
            binding.tvDate.text = date
            binding.tvPrintDate.text = date
            lifecycleScope.launch {
                val allDiscounts = discountDao.getAllDiscounts()
                allDiscounts.forEach { discount ->
                    discount.percentage = 0
                    discount.price = 0.0
                    discountDao.update(discount)
                }
            }
        }
    }

    private fun shareImage(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Share Image"))
    }

    private suspend fun saveBitmapToCache(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val cacheDir = File(cacheDir, "captured_images")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val fileName = "capture_${System.currentTimeMillis()}.png"
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        return@withContext file
    }

    private suspend fun getBitmapFromView(view: View): Bitmap = withContext(Dispatchers.Main) {
        // Ensure RecyclerView has finished rendering
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is RecyclerView) {
                    child.measure(
                        View.MeasureSpec.makeMeasureSpec(child.width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    )
                    child.layout(0, 0, child.measuredWidth, child.measuredHeight)
                }
            }
        }

        // Measure the parent view properly
        val widthSpec = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // Create a bitmap
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return@withContext bitmap
    }


    private fun recreateActivityWithAnimation() {
        lifecycleScope.launch {
            val allDiscounts = discountDao.getAllDiscounts()
            allDiscounts.forEach { discount ->
                discount.percentage = 0
                discount.price = 0.0
                discountDao.update(discount)
            }
        }
        // Create an empty intent to restart the activity
        val intent = Intent(this, this::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        // Start the activity
        startActivity(intent)
        // Add animations for the transition
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        // Finish the current activity
        finish()
    }

    private fun clearInputs(vararg editTexts: EditText) {
        for (editText in editTexts) {
            editText.text.clear()
        }
        // Hide the "Add to List" button after clearing inputs
        binding.addItemButton.visibility = View.GONE
        val defaultQuantity = sharedPreferences.getInt("default_quantity", 4)
        binding.quantityInput.setText("$defaultQuantity")
    }

    private fun fetchAndDisplayDiscounts() {
        lifecycleScope.launch {
            discountList.clear()
            val activeList = discountDao.getAllDiscountsSorted().filter { it.isActive }
            discountList.addAll(activeList)
            discountAdapter.notifyDataSetChanged()
            updateSummary()
            Log.e("check_size", "Size: ${discountList.size}")
        }
    }
    private fun updateSummary() {
        val sumQty = itemList.sumOf { it.quantity }
        binding.sumQty.text = "$sumQty Pcs"

        val sum = itemList.sumOf { it.total }
        var total = sum
        if (discountList.isNotEmpty()) {
            discountAdapter.updateSum(sum)
            discountAdapter.notifyDataSetChanged()
            val activeList = discountList.filter { it.isActive }.sortedBy { it.orderIndex }
            total = activeList.fold(sum) { currentSum, discount ->
                val value = if (discount.percentage > 0) {
                    currentSum * (discount.percentage / 100.0)
                } else {
                    discount.price
                }
                if (discount.isPlus) {
                    currentSum + value
                } else {
                    currentSum - value
                }
            }
        } else {
            discountAdapter.notifyDataSetChanged()
        }
        val roundedSum = sum.roundToInt()
        binding.tvSum.text = String.format(
            Locale.US, "%.2f", roundedSum.toDouble()
        )
        val roundedTotal = total.roundToInt()
        binding.tvFinalTotal.text = String.format(
            Locale.US, "%.2f", roundedTotal.toDouble()
        )
        binding.indexTextView.text = "${itemList.size + 1}."
    }

    private fun addTextWatchers() {
        binding.itemNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        })

        // Add a text watcher to quantityInput
        binding.quantityInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
                calculateTotal()
            }
        })

        // Add a text watcher to rateInput
        binding.rateInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
                calculateTotal()
            }
        })
        binding.rateInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (binding.rateInput.text.isNotEmpty()) {
                    val name = binding.itemNameInput.text.toString().trim()
                    val quantity = binding.quantityInput.text.toString().toIntOrNull() ?: 0
                    val rate = binding.rateInput.text.toString().toDoubleOrNull() ?: 0.00

                    if (name.isNotEmpty() && quantity > 0 && rate > 0) {
                        binding.addItemButton.performClick()
                        binding.rateInput.clearFocus()
                        binding.itemNameInput.requestFocus()
                    }
                } else {
                    Toast.makeText(this, "Enter Rate!", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }
                true // Consume the event
            } else {
                false // Let the system handle other actions
            }
        }
        binding.itemNameInput.setOnEditorActionListener { _, actionId, _ ->
            val itemName = binding.itemNameInput.text.toString().trim()
            if (itemName.isNotEmpty()) {
                lifecycleScope.launch {
                    val rate = rateDatabase.rateDao().getRateByItemName(itemName)
                    if (rate != null) {
                        binding.rateInput.setText(rate.rate.toString())
                    }
                }
            }
            binding.quantityInput.selectAll()
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                if (binding.itemNameInput.text.isNotEmpty()) {
                    binding.quantityInput.requestFocus()
                    return@setOnEditorActionListener true
                } else {
                    Toast.makeText(this, "Enter Item!", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }
            }

            false
        }
        binding.quantityInput.setOnEditorActionListener { _, actionId, _ ->
            if (binding.rateInput.text.isNotEmpty()) {
                binding.rateInput.selectAll()
            }
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                if (binding.quantityInput.text.isNotEmpty()) {
                    binding.rateInput.requestFocus()
                    return@setOnEditorActionListener true
                } else {
                    Toast.makeText(this, "Enter Quantity!", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }
            }
            false
        }
    }

    private fun validateInputs() {
        val name = binding.itemNameInput.text.toString().trim()
        val quantity = binding.quantityInput.text.toString().toIntOrNull() ?: 0
        val rate = binding.rateInput.text.toString().toDoubleOrNull() ?: 0.00

        // Show or hide the "Add to List" button based on input validity
        if (name.isNotEmpty() && quantity > 0 && rate > 0) {
            binding.addItemButton.visibility = View.VISIBLE
        } else {
            binding.addItemButton.visibility = View.GONE
        }
    }

    private fun calculateTotal() {
        val quantity = binding.quantityInput.text.toString().toIntOrNull() ?: 0
        val rate = binding.rateInput.text.toString().toDoubleOrNull() ?: 0.00
        val total = quantity * rate

        binding.tvTotalItem.text = String.format(Locale.US, "%.2f", total)
    }

    override fun onResume() {
        super.onResume()
        val defaultQuantity = sharedPreferences.getInt("default_quantity", 4)
        binding.quantityInput.setText("$defaultQuantity")
        selectedColorCode =
            sharedPreferences.getString("selected_color_code", "#6750A4") ?: "#6750A4"

        binding.llBottomFinal.setBackgroundColor(Color.parseColor(selectedColorCode))
        fetchAndDisplayDiscounts()
    }

    private fun convertTimestampToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun onPerformAction(position: Int, action: DiscountAction) {
        lifecycleScope.launch {
            when (action) {
                DiscountAction.DELETE -> {
                }

                DiscountAction.EDIT -> {
                    showEditDialog(position)
                }

                DiscountAction.ACTIVATE -> {
                }
            }
        }
    }

    private var dialog: AlertDialog? = null
    private fun showEditDialog(position: Int) {
        val layoutInflater = LayoutInflater.from(this)
        val dialogView = EditDiscountDialogBinding.inflate(layoutInflater)

        val currentItem = discountList[position]
        dialogView.etTitle.setText(currentItem.title)
        if (currentItem.percentage > 0) {
            dialogView.etPercentage.setText(currentItem.percentage.toString())
        } else {
            dialogView.etPercentage.setText("")
        }
        if (currentItem.price > 0) {
            dialogView.etPrice.setText(currentItem.price.toString())
        } else {
            dialogView.etPrice.setText("")
        }
        dialogView.isActive.isOn = currentItem.isActive
        dialogView.isAdd.isOn = currentItem.isPlus

        // Add TextWatcher to etPercentage
        dialogView.etPercentage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    dialogView.etPrice.setText("") // Clear price when percentage is typed
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Add TextWatcher to etPrice
        dialogView.etPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    dialogView.etPercentage.setText("") // Clear percentage when price is typed
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        val titleTextView = TextView(this).apply {
            text = "Edit" // Set the title text
            textSize = 18f // Set the text size
            setTextColor(Color.BLACK) // Set the text color to black
            setTypeface(null, Typeface.BOLD) // Make the text bold
            setPadding(0, 30, 0, 10)
            // Optionally, set a custom font
            typeface = ResourcesCompat.getFont(context, R.font.lato_bold)
            gravity = Gravity.CENTER // Center-align the text
        }
        dialog = AlertDialog.Builder(this).setView(dialogView.root).setCustomTitle(titleTextView)
            .setPositiveButton("Save") { _, _ ->
                val title = dialogView.etTitle.text.toString().trim()
                val percentageInput = dialogView.etPercentage.text.toString().trim()
                val priceInput = dialogView.etPrice.text.toString().trim()

//                if (title.isEmpty()) {
//                    Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
//                    return@setPositiveButton
//                }
//
//                if (percentageInput.isEmpty() && priceInput.isEmpty()) {
//                    Toast.makeText(
//                        this, "Please enter either percentage or price", Toast.LENGTH_SHORT
//                    ).show()
//                    return@setPositiveButton
//                }

                // Parse percentage and price
                val percentage =
                    if (percentageInput.isNotEmpty()) percentageInput.toIntOrNull() else 0
                val price = if (priceInput.isNotEmpty()) priceInput.toDoubleOrNull() ?: 0.0 else 0.0

                discountList[position].title = title
                discountList[position].percentage = percentage ?: 0
                discountList[position].price = price
                discountList[position].isPlus = dialogView.isAdd.isOn
                discountList[position].isActive = dialogView.isActive.isOn
                lifecycleScope.launch {
                    discountDao.update(discountList[position])
                    fetchAndDisplayDiscounts()
                }
            }.setNegativeButton("Cancel", null).create()

// Set a white background for the dialog
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))

// Show the dialog
        dialog?.show()
    }
}