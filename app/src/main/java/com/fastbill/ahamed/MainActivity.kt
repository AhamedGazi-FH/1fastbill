package com.fastbill.ahamed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fastbill.ahamed.database.*
import com.fastbill.ahamed.home.MainHomeScreen
import com.fastbill.ahamed.model.TemporaryItem
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: InvoiceViewModel
    private lateinit var sharedPreferences: SharedPreferences

    private val database by lazy { InvoiceDatabase.getDatabase(this) }
    private val customerDao by lazy { database.customerDao() }
    private val rateDao by lazy { database.rateDao() }

    private val syncManager by lazy { SyncManager(this) }
    private val shareManager by lazy { ShareManager(this) }

    private val UPDATE_REQUEST_CODE = 9999
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Toast.makeText(this, "New Update Downloaded! Installing...", Toast.LENGTH_LONG).show()
            appUpdateManager.completeUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // 🚀 EXPERT FIX: Trigger background rate pull instantly on app open
        syncManager.pullRatesSilentlyOnStartup(this)

        val invoiceRepo = InvoiceRepository(database.invoiceDao(), database.itemDao(), database.discountDao(), database.invoiceDiscountDao())
        val prefsRepo = PreferencesRepository(sharedPreferences)
        val factory = InvoiceViewModelFactory(invoiceRepo, prefsRepo)
        viewModel = ViewModelProvider(this, factory)[InvoiceViewModel::class.java]

        try {
            checkAndPerformAutoSync()
            schedulePeriodicBackup()
        } catch (e: Exception) { Log.e("MainActivity", "Error during startup sync: ${e.message}") }

        // --- Load Invoices & History Intends ---
        if (intent.hasExtra("shared_customer_name")) {
            val sharedName = intent.getStringExtra("shared_customer_name") ?: ""
            val adoptedItems = com.fastbill.ahamed.model.SharedDataHolder.itemsToAdopt?.map { TemporaryItem(it.name, it.quantity, it.rate, it.total) } ?: emptyList()
            val adoptedDiscounts = com.fastbill.ahamed.model.SharedDataHolder.discountsToAdopt
            com.fastbill.ahamed.model.SharedDataHolder.itemsToAdopt = null
            com.fastbill.ahamed.model.SharedDataHolder.discountsToAdopt = null
            viewModel.loadInvoiceData(0, adoptedItems, adoptedDiscounts)
            viewModel.setCustomerName(sharedName)
        } else if (intent.hasExtra("invoiceId")) {
            viewModel.loadInvoiceData(intent.getIntExtra("invoiceId", 0))
        } else {
            viewModel.loadInvoiceData(0)
        }

        appUpdateManager.registerListener(installStateUpdatedListener)
        checkAndPromptSmartUpdate()

        // --- 🚀 THE ENTRY POINT (Delegates entirely to MainHomeScreen) ---
        setContent {
            val state by viewModel.uiState.collectAsState()
            val themeColor = try { Color(state.themeColor.toColorInt()) } catch (e: Exception) { Color(0xFF6750A4) }

            LaunchedEffect(themeColor) {
                window.statusBarColor = themeColor.toArgb()
            }

            MaterialTheme(colorScheme = lightColorScheme(primary = themeColor, background = Color.White)) {
                MainHomeScreen(
                    viewModel = viewModel,
                    state = state,
                    customerDao = customerDao,
                    rateDao = rateDao,
                    database = database,
                    sharedPreferences = sharedPreferences,
                    onShareInvoice = { shareInvoice() },
                    onShareFirebase = {
                        val invoice = com.fastbill.ahamed.database.Invoice(name = state.customerName, total = state.grandTotal, timestamp = System.currentTimeMillis())
                        val items = state.items.map { com.fastbill.ahamed.database.Item(name = it.name, quantity = it.quantity, rate = it.rate, total = it.total) }
                        val senderName = sharedPreferences.getString("default_backup_name", "Unknown Sender") ?: "Unknown Sender"
                        shareManager.shareBillTemporarily(invoice, items, state.discounts, senderName)
                    },
                    onSaveInvoice = {
                        if (state.customerName.isNotEmpty() && state.items.isNotEmpty()) {
                            lifecycleScope.launch {
                                if (customerDao.searchCustomers(state.customerName).isEmpty()) customerDao.insertCustomer(Customer(customerName = state.customerName))
                                viewModel.saveInvoice()
                            }
                        } else Toast.makeText(this@MainActivity, "Add items and name first", Toast.LENGTH_SHORT).show()
                    },
                    onResetApp = { recreateActivityWithAnimation() }
                )
            }
        }
    }

    // ==========================================================
    // ⚙️ INVISIBLE ENGINES (Sharing, Sync, App Updates)
    // ==========================================================

    private fun checkAndPerformAutoSync() {
        val lastSync = sharedPreferences.getLong("last_sync_timestamp", 0L)
        if (System.currentTimeMillis() - lastSync > 48 * 60 * 60 * 1000L) {
            lifecycleScope.launch(Dispatchers.IO) {
                val customersOk = syncManager.fetchNewCustomers().isSuccess &&
                                  syncManager.pushUnsyncedCustomers().isSuccess
                val ratesOk = try {
                    syncManager.forceSyncRatesNow(this@MainActivity)
                    true
                } catch (e: Exception) { false }

                if (customersOk && ratesOk) {
                    sharedPreferences.edit { putLong("last_sync_timestamp", System.currentTimeMillis()) }
                }
            }
        }
    }

    private fun schedulePeriodicBackup() {
        val backupRequest = PeriodicWorkRequestBuilder<PeriodicBackupWorker>(10, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("AppBackupJob", ExistingPeriodicWorkPolicy.KEEP, backupRequest)
    }

    private fun shareInvoice() {
        val dateText = SimpleDateFormat("dd-MMM-yy", Locale.getDefault()).format(Date())
        lifecycleScope.launch {
            val bitmap = generateOffScreenInvoiceBitmap(viewModel.uiState.value, dateText)
            val savedUri = saveBitmapToCache(bitmap)
            val isNumberOn = sharedPreferences.getBoolean("share_number_on", false)
            val phone = if (isNumberOn) sharedPreferences.getString("default_share_number", null) else null
            shareImage(savedUri, phone)
            viewModel.saveInvoice()
        }
    }

    private fun shareImage(file: File, phone: String? = null) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val state = viewModel.uiState.value
        if (state.isCaptionOn) {
            val template = state.captionTemplate.ifEmpty { "*Total {qty} Pcs {total}*" }
            val caption = template.replace("{qty}", state.totalQuantity.toString())
                .replace("{total}", "₹ ${NumberFormat.getNumberInstance(Locale("en", "IN")).apply{maximumFractionDigits=0}.format(state.grandTotal.roundToInt())}")
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
            if (isDirectWhatsApp) startActivity(intent) else startActivity(Intent.createChooser(intent, "Share Invoice"))
        } catch (_: Exception) { Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show() }
    }

    private suspend fun saveBitmapToCache(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val cacheDir = File(cacheDir, "captured_images").apply { if (!exists()) mkdirs() }
        val file = File(cacheDir, "capture_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file
    }

    private suspend fun generateOffScreenInvoiceBitmap(state: com.fastbill.ahamed.model.InvoiceUiState, dateText: String): Bitmap = kotlin.coroutines.suspendCoroutine { continuation ->
        val composeView = ComposeView(this@MainActivity)
        composeView.setViewTreeLifecycleOwner(this@MainActivity)
        composeView.setViewTreeViewModelStoreOwner(this@MainActivity)
        composeView.setViewTreeSavedStateRegistryOwner(this@MainActivity)

        composeView.setContent {
            androidx.compose.material3.MaterialTheme {
                InvoicePrintTemplate(
                    customerName = state.customerName, date = dateText, items = state.items, discounts = state.discounts,
                    totalQuantity = state.totalQuantity, subTotal = state.subTotal, grandTotal = state.grandTotal, themeColorHex = state.themeColor,
                    selectedDesign = state.selectedDesign
                )
            }
        }

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val targetWidth = 1080

        composeView.layoutParams = ViewGroup.LayoutParams(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        composeView.x = -10000f
        rootView.addView(composeView)

        composeView.post {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            composeView.measure(widthSpec, heightSpec)
            composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

            if (composeView.measuredWidth > 0 && composeView.measuredHeight > 0) {
                val bitmap = Bitmap.createBitmap(composeView.measuredWidth, composeView.measuredHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(AndroidColor.WHITE)
                composeView.draw(canvas)
                rootView.removeView(composeView)
                composeView.disposeComposition()
                continuation.resume(bitmap)
            } else {
                rootView.removeView(composeView)
                composeView.disposeComposition()
                continuation.resume(Bitmap.createBitmap(targetWidth, 500, Bitmap.Config.ARGB_8888))
            }
        }
    }

    private fun recreateActivityWithAnimation() {
        startActivity(Intent(this, this::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK })
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        finish()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPreferences()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) appUpdateManager.completeUpdate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    private fun checkAndPromptSmartUpdate() {
        val lastPromptTime = sharedPreferences.getLong("last_update_prompt_time", 0L)
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                if (currentTime - lastPromptTime > twentyFourHoursInMillis) {
                    try { appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE, this, UPDATE_REQUEST_CODE) } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            sharedPreferences.edit().putLong("last_update_prompt_time", System.currentTimeMillis()).apply()
        }
    }
}