package com.fastbill.ahamed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fastbill.ahamed.adapter.ItemAdapter
import com.fastbill.ahamed.adapter.ItemDiscountAdapter
import com.fastbill.ahamed.database.Customer
import com.fastbill.ahamed.database.InvoiceDatabase
import com.fastbill.ahamed.database.InvoiceRepository
import com.fastbill.ahamed.database.PeriodicBackupWorker
import com.fastbill.ahamed.database.PreferencesRepository
import com.fastbill.ahamed.database.Rate
import com.fastbill.ahamed.database.ShareManager
import com.fastbill.ahamed.database.SyncManager
import com.fastbill.ahamed.databinding.ActivityMainBinding
import com.fastbill.ahamed.databinding.EditDiscountDialogBinding
import com.fastbill.ahamed.model.DiscountAction
import com.fastbill.ahamed.model.TemporaryItem
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: InvoiceViewModel
    private lateinit var adapter: ItemAdapter
    private lateinit var discountAdapter: ItemDiscountAdapter
    lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    private val database by lazy { InvoiceDatabase.getDatabase(this) }
    private val customerDao by lazy { database.customerDao() }
    private val rateDao by lazy { database.rateDao() }

    private val syncManager by lazy { SyncManager(this) }
    private val shareManager by lazy { ShareManager(this) }

    private lateinit var customerAdapter: ArrayAdapter<String>
    private var invoiceId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
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

        val invoiceRepo = InvoiceRepository(database.invoiceDao(), database.itemDao(), database.discountDao(), database.invoiceDiscountDao())
        val prefsRepo = PreferencesRepository(sharedPreferences)
        val factory = InvoiceViewModelFactory(invoiceRepo, prefsRepo)
        viewModel = ViewModelProvider(this, factory)[InvoiceViewModel::class.java]

        try {
            checkAndPerformAutoSync()
            schedulePeriodicBackup()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during startup sync/backup init: ${e.message}")
        }

        setupToolbarActions()
        setupAdapters()
        setupStateObservation()
        setupInputLogic()
        setupCustomerAutocomplete()

        val defaultQuantity = sharedPreferences.getInt("default_quantity", 4)
        binding.quantityInput.setText(defaultQuantity.toString())

        if (intent.hasExtra("shared_customer_name")) {
            val sharedName = intent.getStringExtra("shared_customer_name") ?: ""
            binding.edtUsername.setText(sharedName)

            val adoptedItems = com.fastbill.ahamed.model.SharedDataHolder.itemsToAdopt?.map { item ->
                TemporaryItem(item.name, item.quantity, item.rate, item.total)
            } ?: emptyList()
            val adoptedDiscounts = com.fastbill.ahamed.model.SharedDataHolder.discountsToAdopt

            com.fastbill.ahamed.model.SharedDataHolder.itemsToAdopt = null
            com.fastbill.ahamed.model.SharedDataHolder.discountsToAdopt = null

            invoiceId = 0
            viewModel.loadInvoiceData(0, adoptedItems, adoptedDiscounts)
            viewModel.setCustomerName(sharedName)

            val date = convertTimestampToDate(System.currentTimeMillis())
            binding.tvDate.text = date
            binding.tvPrintDate.text = date

        } else if (intent.hasExtra("invoiceId")) {
            invoiceId = intent.getIntExtra("invoiceId", 0)
            viewModel.loadInvoiceData(invoiceId)

            val date = convertTimestampToDate(System.currentTimeMillis())
            binding.tvDate.text = date
            binding.tvPrintDate.text = date

        } else {
            viewModel.loadInvoiceData(0)
            val date = convertTimestampToDate(System.currentTimeMillis())
            binding.tvDate.text = date
            binding.tvPrintDate.text = date
        }
    }

    private fun setupStateObservation() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.updateData(state.items)
                    discountAdapter.updateData(state.discounts, state.subTotal)

                    val indianFormat = java.text.NumberFormat.getNumberInstance(Locale("en", "IN"))
                    binding.sumQty.text = "${state.totalQuantity} Pcs"
                    binding.tvSum.text = "₹ ${indianFormat.format(state.subTotal.roundToInt())}"
                    binding.tvFinalTotal.text = "₹ ${indianFormat.format(state.grandTotal.roundToInt())}"
                    binding.indexTextView.text = "${state.items.size + 1}."

                    binding.loading.loading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.tvName.text = state.customerName

                    if (binding.edtUsername.text.toString().isEmpty() && state.customerName.isNotEmpty()) {
                        binding.edtUsername.setText(state.customerName)
                    }

                    if (state.invoiceId != 0) {
                        binding.btnSave.text = getString(R.string.update)
                        binding.imgShareFirebase.visibility = View.VISIBLE
                    } else {
                        binding.btnSave.text = getString(R.string.save)
                        binding.imgShareFirebase.visibility = View.GONE
                    }

                    if (state.isSaved) {
                        Toast.makeText(this@MainActivity, "Invoice Saved Successfully", Toast.LENGTH_SHORT).show()
                        viewModel.resetSaveState()
                    }

                    // Observe Real-time Colors
                    try {
                        val parsedColor = Color.parseColor(state.themeColor)
                        window.statusBarColor = parsedColor
                        binding.llBottomFinal.setBackgroundColor(parsedColor)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Invalid theme color: ${state.themeColor}")
                    }
                }
            }
        }
    }

    private fun setupAdapters() {
        adapter = ItemAdapter(
            emptyList(),
            onEditItem = { pos, item -> viewModel.updateItem(pos, item) },
            onDeleteItem = { pos -> viewModel.removeItem(pos) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        discountAdapter = ItemDiscountAdapter(emptyList(), ::onPerformAction, 0.0)
        binding.rvDiscount.layoutManager = LinearLayoutManager(this)
        binding.rvDiscount.adapter = discountAdapter

        val itemTouchHelperCallback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                return makeMovementFlags(dragFlags, 0)
            }
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                viewModel.swapDiscounts(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvDiscount)
    }

    private fun setupInputLogic() {
        binding.addItemButton.setOnClickListener {
            val name = binding.itemNameInput.text?.toString()?.trim() ?: ""
            val quantity = binding.quantityInput.text?.toString()?.toIntOrNull() ?: 0
            val rate = binding.rateInput.text?.toString()?.toDoubleOrNull() ?: 0.00
            val total = quantity * rate

            if (name.isNotEmpty() && quantity > 0 && rate > 0) {
                viewModel.addItem(TemporaryItem(name, quantity, rate, total))
                clearInputs(binding.itemNameInput, binding.rateInput)

                lifecycleScope.launch {
                    val existingRate = rateDao.getRateByItemName(name)
                    if (existingRate != null) {
                        existingRate.rate = rate
                        rateDao.update(existingRate)
                    } else {
                        rateDao.insert(Rate(item_name = name, rate = rate))
                    }
                }
            } else {
                Toast.makeText(this, "Please enter valid values", Toast.LENGTH_SHORT).show()
            }
        }

        binding.edtUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setCustomerName(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        addTextWatchers()
    }

    private fun setupToolbarActions() {
        binding.imgReset.setOnClickListener { recreateActivityWithAnimation() }
        binding.imgInvoice.setOnClickListener { startActivity(Intent(this, InvoiceActivity::class.java)) }
        binding.imgSetting.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            intent.putExtra("invoiceId", viewModel.uiState.value.invoiceId)
            startActivity(intent)
        }

        binding.btnQuickSetup.setOnClickListener { showQuickSetupBottomSheet() }
        binding.btnShare.setOnLongClickListener { showQuickShareSettings(); true }

        binding.btnSave.setOnClickListener {
            val name = viewModel.uiState.value.customerName
            if (name.isNotEmpty() && viewModel.uiState.value.items.isNotEmpty()) {
                lifecycleScope.launch {
                    val existing = customerDao.searchCustomers(name)
                    if (existing.isEmpty()) { customerDao.insertCustomer(Customer(customerName = name)) }
                    viewModel.saveInvoice()
                }
            } else { Toast.makeText(this, "Please enter name and add items", Toast.LENGTH_SHORT).show() }
        }

        binding.btnShare.setOnClickListener {
            if (viewModel.uiState.value.customerName.isNotEmpty() && viewModel.uiState.value.items.isNotEmpty()) {
                shareInvoice()
            } else { Toast.makeText(this, "Add items and name first", Toast.LENGTH_SHORT).show() }
        }

        binding.imgShareFirebase.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.customerName.isNotEmpty() && state.items.isNotEmpty()) {
                val invoice = com.fastbill.ahamed.database.Invoice(name = state.customerName, total = state.grandTotal, timestamp = System.currentTimeMillis())
                val items = state.items.map { com.fastbill.ahamed.database.Item(name = it.name, quantity = it.quantity, rate = it.rate, total = it.total) }
                val senderName = sharedPreferences.getString("default_backup_name", "Unknown Sender") ?: "Unknown Sender"
                shareManager.shareBillTemporarily(invoice, items, state.discounts, senderName)
            }
        }
    }

    private fun shareInvoice() {
        if (binding.tvDate.text.isNullOrEmpty()) binding.tvDate.text = convertTimestampToDate(System.currentTimeMillis())
        binding.tvPrintDate.text = binding.tvDate.text

        binding.loading.loading.visibility = View.VISIBLE
        binding.relUser.visibility = View.VISIBLE
        binding.btnQuickSetup.visibility = View.GONE

        lifecycleScope.launch {
            val bitmap = createBitmapFromView(binding.receiptRootContainer)
            val savedUri = saveBitmapToCache(bitmap)

            val state = viewModel.uiState.value
            var phone: String? = null
            if (state.isNumberOn) {
                phone = state.defaultShareNumber.ifEmpty {
                    state.shareNumber1.ifEmpty {
                        state.shareNumber2.ifEmpty {
                            state.shareNumber3.ifEmpty { null }
                        }
                    }
                }
            }

            shareImage(savedUri, phone)

            viewModel.saveInvoice()
            withContext(Dispatchers.Main) {
                binding.relUser.visibility = View.GONE
                binding.loading.loading.visibility = View.GONE
                binding.btnQuickSetup.visibility = View.VISIBLE
            }
        }
    }

    private fun onPerformAction(position: Int, action: DiscountAction) {
        when (action) {
            DiscountAction.DELETE -> {
                val discountId = viewModel.uiState.value.discounts[position].id
                viewModel.removeDiscount(discountId)
            }
            DiscountAction.EDIT -> showEditDialog(position)
            DiscountAction.ACTIVATE -> {}
        }
    }

    // ======================================================================
    // 👑 EXPERT SAAS UI - QUICK SETUP
    // ======================================================================
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    private fun showQuickSetupBottomSheet() {
        val dialog = BottomSheetDialog(this, R.style.Theme_Design_Light_BottomSheetDialog)

        val composeView = ComposeView(this).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsState()
                val themeColor = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(uiState.themeColor)) } catch (e: Exception) { androidx.compose.ui.graphics.Color(0xFF6750A4) }

                MaterialTheme(colorScheme = lightColorScheme(primary = themeColor)) {
                    var discounts by remember { mutableStateOf<List<com.fastbill.ahamed.database.Discount>>(emptyList()) }

                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            discounts = database.discountDao().getAllDiscountsSorted()
                        }
                    }

                    val defaultColors = "#6750A4,#FFC107,#3F51B5,#4CAF50"
                    val rawColors = sharedPreferences.getString("custom_color_list", defaultColors) ?: defaultColors
                    val colorList = rawColors.split(",").take(4)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(androidx.compose.ui.graphics.Color.White)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(0xFFE5E7EB))
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Invoice Setup", fontWeight = FontWeight.Black, fontSize = 22.sp, color = androidx.compose.ui.graphics.Color(0xFF111827))
                        Text("Manage templates and branding for this bill.", fontSize = 14.sp, color = androidx.compose.ui.graphics.Color(0xFF6B7280), modifier = Modifier.padding(bottom = 24.dp))

                        // --- 1. QUICK ADD TEMPLATES
                        val negativeDiscounts = discounts.filter { !it.isPlus }
                        val positiveDiscounts = discounts.filter { it.isPlus }

                        if (negativeDiscounts.isNotEmpty()) {
                            Text("Discounts Applied", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF9CA3AF), letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                negativeDiscounts.forEach { discount ->
                                    val isSelected = uiState.discounts.any { it.id == discount.id }
                                    val valText = if (discount.percentage > 0) "${discount.percentage}%" else "₹${discount.price.toInt()}"
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { if (!isSelected) viewModel.addDiscount(discount) else viewModel.removeDiscount(discount.id) },
                                        label = { Text("${discount.title} ($valText)", fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = androidx.compose.ui.graphics.Color(0xFFFEE2E2),
                                            selectedLabelColor = androidx.compose.ui.graphics.Color(0xFFB91C1C),
                                            containerColor = androidx.compose.ui.graphics.Color(0xFFF3F4F6)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        border = null
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        if (positiveDiscounts.isNotEmpty()) {
                            Text("Charges Added", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF9CA3AF), letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                positiveDiscounts.forEach { discount ->
                                    val isSelected = uiState.discounts.any { it.id == discount.id }
                                    val valText = if (discount.percentage > 0) "${discount.percentage}%" else "₹${discount.price.toInt()}"
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { if (!isSelected) viewModel.addDiscount(discount) else viewModel.removeDiscount(discount.id) },
                                        label = { Text("${discount.title} ($valText)", fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = androidx.compose.ui.graphics.Color(0xFFD1FAE5),
                                            selectedLabelColor = androidx.compose.ui.graphics.Color(0xFF047857),
                                            containerColor = androidx.compose.ui.graphics.Color(0xFFF3F4F6)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        border = null
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFFF3F4F6), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(24.dp))

                        // --- 2. THEME COLOR
                        Text("Brand Color", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF9CA3AF), letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            colorList.forEach { colorStr ->
                                val parsedColor = try { android.graphics.Color.parseColor(colorStr) } catch(e: Exception) { android.graphics.Color.GRAY }
                                val isSelected = colorStr == uiState.themeColor
                                val targetSize by animateDpAsState(if (isSelected) 44.dp else 36.dp, label = "color_size")

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            sharedPreferences.edit().putString("selected_color_code", colorStr).apply()
                                            viewModel.refreshPreferences()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(targetSize)
                                            .clip(CircleShape)
                                            .background(androidx.compose.ui.graphics.Color(parsedColor))
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Done Button
                        Button(
                            onClick = { dialog.dismiss() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF111827))
                        ) {
                            Text("DONE", fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.show()
    }

    // ======================================================================
    // 👑 EXPERT SAAS UI - QUICK SHARE
    // ======================================================================
    @OptIn(ExperimentalMaterial3Api::class)
    private fun showQuickShareSettings() {
        val dialog = BottomSheetDialog(this, R.style.Theme_Design_Light_BottomSheetDialog)

        val composeView = ComposeView(this).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsState()
                val themeColor = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(uiState.themeColor)) } catch (e: Exception) { androidx.compose.ui.graphics.Color(0xFF6750A4) }

                MaterialTheme(colorScheme = lightColorScheme(primary = themeColor)) {
                    var shareApp by remember { mutableStateOf(sharedPreferences.getString("share_app", "other") ?: "other") }
                    var captionOn by remember { mutableStateOf(sharedPreferences.getBoolean("share_caption", true)) }
                    var numberOn by remember { mutableStateOf(sharedPreferences.getBoolean("share_number_on", false)) }

                    var defaultNumber by remember {
                        val firstAvailable = uiState.shareNumber1.takeIf { it.isNotEmpty() }
                            ?: uiState.shareNumber2.takeIf { it.isNotEmpty() }
                            ?: uiState.shareNumber3.takeIf { it.isNotEmpty() } ?: ""
                        mutableStateOf(sharedPreferences.getString("default_share_number", firstAvailable) ?: firstAvailable)
                    }

                    val numbers = listOfNotNull(
                        uiState.shareNumber1.takeIf { it.isNotEmpty() },
                        uiState.shareNumber2.takeIf { it.isNotEmpty() },
                        uiState.shareNumber3.takeIf { it.isNotEmpty() }
                    )

                    Column(modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.White).padding(horizontal = 24.dp, vertical = 16.dp)) {

                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(0xFFE5E7EB))
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Share Document", fontWeight = FontWeight.Black, fontSize = 22.sp, color = androidx.compose.ui.graphics.Color(0xFF111827), modifier = Modifier.padding(bottom = 20.dp))

                        // App Selector
                        Surface(
                            color = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                listOf("wa" to "WhatsApp", "wa_biz" to "WA Biz", "other" to "Other").forEach { (code, label) ->
                                    val isSel = shareApp == code
                                    Surface(
                                        color = if (isSel) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                        shadowElevation = if (isSel) 2.dp else 0.dp,
                                        modifier = Modifier.weight(1f).clickable {
                                            shareApp = code
                                            sharedPreferences.edit().putString("share_app", code).apply()
                                            viewModel.refreshPreferences()
                                        }
                                    ) {
                                        Text(label, color = if (isSel) androidx.compose.ui.graphics.Color(0xFF111827) else androidx.compose.ui.graphics.Color(0xFF6B7280), fontSize = 14.sp, fontWeight = if(isSel) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Settings Container
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE5E7EB)),
                            color = androidx.compose.ui.graphics.Color.White
                        ) {
                            Column {
                                // Caption Toggle
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Auto-Caption", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF111827))
                                        Text("Include details text in message", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFF6B7280))
                                    }
                                    Switch(checked = captionOn, onCheckedChange = {
                                        captionOn = it;
                                        sharedPreferences.edit().putBoolean("share_caption", it).apply()
                                        viewModel.refreshPreferences()
                                    }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                                }

                                HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFFF3F4F6))

                                // Direct Number Toggle
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Direct Send", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF111827))
                                        Text("Skip contact selection screen", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFF6B7280))
                                    }
                                    Switch(checked = numberOn, onCheckedChange = {
                                        numberOn = it;
                                        sharedPreferences.edit().putBoolean("share_number_on", it).apply()
                                        viewModel.refreshPreferences()
                                    }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                                }
                            }
                        }

                        AnimatedVisibility(visible = numberOn && numbers.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text("Select Destination", color = androidx.compose.ui.graphics.Color(0xFF6B7280), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    numbers.forEach { num ->
                                        Surface(
                                            color = if (defaultNumber == num) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color(0xFFF9FAFB),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, if (defaultNumber == num) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(0xFFE5E7EB)),
                                            modifier = Modifier.clickable {
                                                defaultNumber = num;
                                                sharedPreferences.edit().putString("default_share_number", num).apply()
                                                viewModel.refreshPreferences()
                                            }
                                        ) {
                                            Text(num, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.Bold, color = if (defaultNumber == num) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(0xFF374151))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Primary Action
                        Button(
                            onClick = {
                                if (viewModel.uiState.value.customerName.isNotEmpty() && viewModel.uiState.value.items.isNotEmpty()) {
                                    if (binding.tvDate.text.isNullOrEmpty()) binding.tvDate.text = convertTimestampToDate(System.currentTimeMillis())
                                    binding.tvPrintDate.text = binding.tvDate.text

                                    binding.loading.loading.visibility = View.VISIBLE
                                    binding.relUser.visibility = View.VISIBLE
                                    binding.btnQuickSetup.visibility = View.GONE

                                    lifecycleScope.launch {
                                        val bitmap = createBitmapFromView(binding.receiptRootContainer)
                                        val savedUri = saveBitmapToCache(bitmap)

                                        var phone: String? = null
                                        if (numberOn) {
                                            phone = defaultNumber.takeIf { it.isNotEmpty() }
                                                ?: uiState.shareNumber1.takeIf { it.isNotEmpty() }
                                                        ?: uiState.shareNumber2.takeIf { it.isNotEmpty() }
                                                        ?: uiState.shareNumber3.takeIf { it.isNotEmpty() }
                                        }

                                        shareImage(savedUri, phone)
                                        viewModel.saveInvoice()

                                        withContext(Dispatchers.Main) {
                                            binding.relUser.visibility = View.GONE
                                            binding.loading.loading.visibility = View.GONE
                                            binding.btnQuickSetup.visibility = View.VISIBLE
                                            dialog.dismiss()
                                        }
                                    }
                                } else {
                                    Toast.makeText(this@MainActivity, "Add items and name first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                            Text("QUICK SHARE NOW", fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.show()
    }

    private fun checkAndPerformAutoSync() {
        val lastSync = sharedPreferences.getLong("last_sync_timestamp", 0L)
        if (System.currentTimeMillis() - lastSync > 48 * 60 * 60 * 1000L) {
            lifecycleScope.launch(Dispatchers.IO) {
                if (syncManager.fetchNewCustomers().isSuccess && syncManager.pushUnsyncedCustomers().isSuccess) {
                    sharedPreferences.edit().putLong("last_sync_timestamp", System.currentTimeMillis()).apply()
                }
            }
        }
    }

    private fun schedulePeriodicBackup() {
        val backupRequest = PeriodicWorkRequestBuilder<PeriodicBackupWorker>(10, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("AppBackupJob", ExistingPeriodicWorkPolicy.KEEP, backupRequest)
    }

    private fun setupCustomerAutocomplete() {
        customerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        binding.edtUsername.setAdapter(customerAdapter)
        binding.edtUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    lifecycleScope.launch {
                        val names = customerDao.searchCustomers("%$query%").map { it.customerName }
                        customerAdapter.clear()
                        customerAdapter.addAll(names)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun shareImage(file: File, phone: String? = null) {
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val state = viewModel.uiState.value
        if (state.isCaptionOn) {
            val template = state.captionTemplate.ifEmpty { "*Total {qty} Pcs {total}*" }
            val caption = template.replace("{qty}", state.totalQuantity.toString())
                .replace("{total}", binding.tvFinalTotal.text.toString().trim())
                .replace("{name}", state.customerName)
            intent.putExtra(Intent.EXTRA_TEXT, caption)
        }

        val shareApp = sharedPreferences.getString("share_app", "other")
        if (shareApp == "wa") intent.setPackage("com.whatsapp")
        else if (shareApp == "wa_biz") intent.setPackage("com.whatsapp.w4b")

        var isDirectWhatsApp = false
        if (!phone.isNullOrEmpty() && (intent.`package` == "com.whatsapp" || intent.`package` == "com.whatsapp.w4b")) {
            val formattedPhone = phone.replace("+", "").replace(" ", "")
            if (formattedPhone.isNotEmpty()) {
                val finalPhone = if (formattedPhone.length == 10) "91$formattedPhone" else formattedPhone
                intent.putExtra("jid", "$finalPhone@s.whatsapp.net")
                isDirectWhatsApp = true
            }
        }

        try {
            if (isDirectWhatsApp) {
                startActivity(intent)
            } else {
                startActivity(Intent.createChooser(intent, "Share Invoice"))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "App not installed or error sharing", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun saveBitmapToCache(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val cacheDir = File(cacheDir, "captured_images").apply { if (!exists()) mkdirs() }
        val file = File(cacheDir, "capture_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file
    }

    private fun createBitmapFromView(view: View): Bitmap {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        view.draw(canvas)
        return bitmap
    }

    private fun recreateActivityWithAnimation() {
        startActivity(Intent(this, this::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK })
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        finish()
    }

    private fun clearInputs(vararg editTexts: EditText) {
        editTexts.forEach { it.text.clear() }
        binding.addItemButton.visibility = View.GONE
        binding.quantityInput.setText(sharedPreferences.getInt("default_quantity", 4).toString())
    }

    private fun addTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateInputs()
                calculateTotal()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.itemNameInput.addTextChangedListener(watcher)
        binding.quantityInput.addTextChangedListener(watcher)
        binding.rateInput.addTextChangedListener(watcher)

        binding.rateInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.addItemButton.performClick()
                binding.itemNameInput.requestFocus()
                true
            } else false
        }
        binding.itemNameInput.setOnEditorActionListener { _, actionId, _ ->
            val itemName = binding.itemNameInput.text?.toString()?.trim() ?: ""
            if (itemName.isNotEmpty()) {
                lifecycleScope.launch {
                    rateDao.getRateByItemName(itemName)?.let { binding.rateInput.setText(it.rate.toString()) }
                }
            }
            binding.quantityInput.selectAll()
            binding.quantityInput.requestFocus()
            true
        }
    }

    private fun validateInputs() {
        val name = binding.itemNameInput.text?.toString()?.trim() ?: ""
        val qty = binding.quantityInput.text?.toString()?.toIntOrNull() ?: 0
        val rate = binding.rateInput.text?.toString()?.toDoubleOrNull() ?: 0.0
        binding.addItemButton.visibility = if (name.isNotEmpty() && qty > 0 && rate > 0.0) View.VISIBLE else View.GONE
    }

    private fun calculateTotal() {
        val qty = binding.quantityInput.text?.toString()?.toIntOrNull() ?: 0
        val rate = binding.rateInput.text?.toString()?.toDoubleOrNull() ?: 0.0
        binding.tvTotalItem.text = String.format(Locale.US, "%.2f", qty * rate)
    }

    private fun convertTimestampToDate(timestamp: Long): String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(timestamp))

    override fun onResume() {
        super.onResume()
        viewModel.refreshPreferences()
    }

    private fun showEditDialog(position: Int) {
        val dialogBinding = EditDiscountDialogBinding.inflate(layoutInflater)
        val current = viewModel.uiState.value.discounts[position]

        dialogBinding.tvActiveLabel.text = "Apply to Current Bill"
        dialogBinding.switchIsActive.isChecked = true

        dialogBinding.etTitle.setText(current.title)
        dialogBinding.etPercentage.setText(if (current.percentage > 0) current.percentage.toString() else "")
        dialogBinding.etPrice.setText(if (current.price > 0) current.price.toString() else "")
        dialogBinding.rgIsAdd.check(if (current.isPlus) R.id.rb_plus else R.id.rb_minus)

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                if (!dialogBinding.switchIsActive.isChecked) {
                    viewModel.removeDiscountAt(position)
                } else {
                    val updated = current.copy(
                        title = dialogBinding.etTitle.text.toString(),
                        percentage = dialogBinding.etPercentage.text.toString().toIntOrNull() ?: 0,
                        price = dialogBinding.etPrice.text.toString().toDoubleOrNull() ?: 0.0,
                        isPlus = dialogBinding.rgIsAdd.checkedRadioButtonId == R.id.rb_plus,
                        isActive = true
                    )
                    viewModel.updateDiscount(position, updated)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is android.widget.EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
