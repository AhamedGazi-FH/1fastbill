package com.fastbill.ahamed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.fastbill.ahamed.R
import com.fastbill.ahamed.adapter.ItemDiscountSettingAdapter
import com.fastbill.ahamed.adapter.ManageCustomerAdapter
import com.fastbill.ahamed.database.BackupManager
import com.fastbill.ahamed.database.CSVImporter
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.database.InvoiceDatabase
import com.fastbill.ahamed.database.PeriodicBackupWorker
import com.fastbill.ahamed.database.SyncManager
import com.fastbill.ahamed.databinding.ActivitySettingBinding
import com.fastbill.ahamed.databinding.EditDiscountDialogBinding
import com.fastbill.ahamed.model.DiscountAction
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SettingActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingBinding
    private lateinit var discountAdapter: ItemDiscountSettingAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var discountList: MutableList<Discount>
    private lateinit var colorList: MutableList<String>
    private var selectedColorCode = "#6750A4"
    private var currentInvoiceId: Int? = null
    private val database by lazy { InvoiceDatabase.getDatabase(this) }
    private val discountDao by lazy { database.discountDao() }
    private val customerDao by lazy { database.customerDao() }
    private val syncLogDao by lazy { database.syncLogDao() }
    private val syncManager by lazy { SyncManager(this) }
    private val backupManager by lazy { BackupManager(this) }

    private val csvPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val result = CSVImporter.importCustomersFromCSV(this@SettingActivity, it, customerDao)
                result.onSuccess { count ->
                    Toast.makeText(this@SettingActivity, "Imported $count customers successfully", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(this@SettingActivity, "Import failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.imgBack.setOnClickListener { finish() }

        currentInvoiceId = intent.getIntExtra("invoiceId", -1).takeIf { it != -1 }
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        loadSettings()
        setupAutoSaveListeners()
        setupDiscountLogic()
        setupColorLogic()
        setupCloudActions()
    }

    private fun loadSettings() {
        // Load Share Settings
        val shareApp = sharedPreferences.getString("share_app", "other")
        when (shareApp) {
            "wa" -> binding.rgShareApp.check(R.id.rb_wa)
            "wa_biz" -> binding.rgShareApp.check(R.id.rb_wa_biz)
            else -> binding.rgShareApp.check(R.id.rb_other)
        }

        val isCaptionOn = sharedPreferences.getBoolean("share_caption", true)
        binding.switchCaption.isChecked = isCaptionOn
        binding.tilCaption.visibility = if (isCaptionOn) View.VISIBLE else View.GONE
        binding.etCaptionTemplate.setText(sharedPreferences.getString("share_caption_template", "*Total {qty} Pcs {total}*"))

        val isNumberOn = sharedPreferences.getBoolean("share_number_on", false)
        binding.switchNumber.isChecked = isNumberOn
        binding.tilNumber.visibility = if (isNumberOn) View.VISIBLE else View.GONE

        // Load triple numbers
        binding.etNumber1.setText(sharedPreferences.getString("share_number_1", ""))
        binding.etNumber2.setText(sharedPreferences.getString("share_number_2", ""))
        binding.etNumber3.setText(sharedPreferences.getString("share_number_3", ""))

        binding.etBackupDays.setText(sharedPreferences.getInt("auto_backup_days", 10).toString())
        binding.quantityInput.setText(sharedPreferences.getInt("default_quantity", 4).toString())
    }

    private fun setupAutoSaveListeners() {
        binding.quantityInput.doAfterTextChanged { s ->
            val q = s.toString().toIntOrNull() ?: 0
            if (q > 0) sharedPreferences.edit { putInt("default_quantity", q) }
        }

        binding.rgShareApp.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedShareApp = when (checkedIds.firstOrNull()) {
                R.id.rb_wa -> "wa"
                R.id.rb_wa_biz -> "wa_biz"
                else -> "other"
            }
            sharedPreferences.edit { putString("share_app", selectedShareApp) }
        }

        binding.switchCaption.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean("share_caption", isChecked) }
            binding.tilCaption.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.etCaptionTemplate.doAfterTextChanged { s ->
            sharedPreferences.edit { putString("share_caption_template", s.toString().trim()) }
        }

        binding.switchNumber.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean("share_number_on", isChecked) }
            binding.tilNumber.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.etNumber1.doAfterTextChanged { s ->
            sharedPreferences.edit { putString("share_number_1", s.toString().trim()) }
        }
        binding.etNumber2.doAfterTextChanged { s ->
            sharedPreferences.edit { putString("share_number_2", s.toString().trim()) }
        }
        binding.etNumber3.doAfterTextChanged { s ->
            sharedPreferences.edit { putString("share_number_3", s.toString().trim()) }
        }

        binding.etBackupDays.doAfterTextChanged { s ->
            val days = s.toString().toIntOrNull() ?: 10
            if (days > 0) {
                sharedPreferences.edit { putInt("auto_backup_days", days) }
                rescheduleBackup(days)
            }
        }

        // Redundant SAVE button repurposed for better feedback
        binding.btnUpdate.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.quantityInput.windowToken, 0)
            binding.quantityInput.clearFocus()
            Toast.makeText(this, "Settings auto-saved.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDiscountLogic() {
        binding.etPercentage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) binding.etPrice.setText("")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) binding.etPercentage.setText("")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.rgIsAdd.check(R.id.rb_plus)
        binding.switchIsActive.isChecked = true

        binding.btnAdd.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val percentageInput = binding.etPercentage.text.toString().trim()
            val priceInput = binding.etPrice.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (percentageInput.isEmpty() && priceInput.isEmpty()) {
                Toast.makeText(this, "Please enter either percentage or price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val percentage = percentageInput.toIntOrNull() ?: 0
            val price = priceInput.toDoubleOrNull() ?: 0.0
            val isPlus = binding.rgIsAdd.checkedButtonId == R.id.rb_plus
            val isActive = binding.switchIsActive.isChecked

            val discount = Discount(title = title, percentage = percentage, price = price, isPlus = isPlus, isActive = isActive, orderIndex = discountList.size)

            lifecycleScope.launch {
                discountDao.insert(discount)
                Toast.makeText(this@SettingActivity, "Record added successfully", Toast.LENGTH_SHORT).show()
                fetchAndDisplayDiscounts()
                binding.etTitle.text?.clear()
                binding.etPercentage.text?.clear()
                binding.etPrice.text?.clear()
                binding.etTitle.requestFocus()
            }
        }

        discountList = mutableListOf()
        discountAdapter = ItemDiscountSettingAdapter(discountList, ::onPerformAction)
        binding.rvDiscount.layoutManager = LinearLayoutManager(this)
        binding.rvDiscount.adapter = discountAdapter
        fetchAndDisplayDiscounts()
    }

    private fun setupColorLogic() {
        selectedColorCode = sharedPreferences.getString("selected_color_code", "#6750A4") ?: "#6750A4"
        loadColorView()
        binding.btnChangeTheme.setOnClickListener {
            showChangeColorDialog(colorList.indexOf(selectedColorCode))
        }
    }

    private fun setupCloudActions() {
        binding.btnImportCsv.setOnClickListener { csvPickerLauncher.launch("text/comma-separated-values") }
        binding.btnManageCustomers.setOnClickListener { showManageCustomersDialog() }
        binding.btnForceSync.setOnClickListener {
            lifecycleScope.launch {
                val fetchResult = syncManager.fetchNewCustomers()
                val pushResult = syncManager.pushUnsyncedCustomers()
                if (fetchResult.isSuccess && pushResult.isSuccess) {
                    sharedPreferences.edit().putLong("last_sync_timestamp", System.currentTimeMillis()).apply()
                    Toast.makeText(this@SettingActivity, "Sync Complete", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingActivity, "Sync Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.btnForceSync.setOnLongClickListener { showSyncLogs(); true }
        binding.btnManualBackup.setOnClickListener {
            val defaultName = sharedPreferences.getString("default_backup_name", null)
            if (defaultName != null) performBackup(defaultName) else showBackupNameDialog()
        }
        binding.btnChangeBackupName.setOnClickListener { showBackupNameDialog() }
        binding.btnRestoreBackup.setOnClickListener {
            lifecycleScope.launch {
                binding.loadingLayout.loading.visibility = View.VISIBLE
                binding.loadingLayout.loading.playAnimation()
                val result = backupManager.getAvailableProfilesWithDates()
                binding.loadingLayout.loading.visibility = View.GONE
                binding.loadingLayout.loading.cancelAnimation()

                result.onSuccess { profiles ->
                    if (profiles.isEmpty()) {
                        Toast.makeText(this@SettingActivity, "No backups found", Toast.LENGTH_SHORT).show()
                        return@onSuccess
                    }
                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    val displayList = profiles.map { (name, timestamp) ->
                        val dateStr = if (timestamp > 0) sdf.format(Date(timestamp)) else "Unknown Date"
                        "$name ($dateStr)"
                    }
                    AlertDialog.Builder(this@SettingActivity)
                        .setTitle("Select Profile to Restore")
                        .setItems(displayList.toTypedArray()) { _, which -> showRestoreOptionsDialog(profiles[which].first) }
                        .show()
                }.onFailure {
                    Toast.makeText(this@SettingActivity, "Failed to fetch profiles: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun rescheduleBackup(days: Int) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val backupRequest = PeriodicWorkRequestBuilder<PeriodicBackupWorker>(days.toLong(), TimeUnit.DAYS).setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("AppBackupJob", ExistingPeriodicWorkPolicy.UPDATE, backupRequest)
    }

    private fun showBackupNameDialog() {
        val input = EditText(this)
        input.hint = "e.g., Mahmood"
        input.setText(sharedPreferences.getString("default_backup_name", ""))
        AlertDialog.Builder(this).setTitle("Enter Profile Name").setView(input).setPositiveButton("Save") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                sharedPreferences.edit().putString("default_backup_name", name).apply()
                performBackup(name)
            }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun performBackup(name: String) {
        lifecycleScope.launch {
            binding.loadingLayout.loading.visibility = View.VISIBLE
            binding.loadingLayout.loading.playAnimation()
            val result = backupManager.createAndUploadSnapshot(name)
            binding.loadingLayout.loading.visibility = View.GONE
            binding.loadingLayout.loading.cancelAnimation()
            if (result.isSuccess) Toast.makeText(this@SettingActivity, "Backup Successful", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this@SettingActivity, "Backup Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRestoreOptionsDialog(profileName: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_restore_options, null)
        val cbBills = dialogView.findViewById<CheckBox>(R.id.cbRestoreBills)
        val cbSettings = dialogView.findViewById<CheckBox>(R.id.cbRestoreSettings)
        AlertDialog.Builder(this).setTitle("Restore Options for $profileName").setView(dialogView).setPositiveButton("Apply") { _, _ ->
            lifecycleScope.launch {
                binding.loadingLayout.loading.visibility = View.VISIBLE
                binding.loadingLayout.loading.playAnimation()
                val snapshot = backupManager.downloadSnapshot(profileName)
                if (snapshot != null) {
                    backupManager.applySelectiveRestore(snapshot, cbBills.isChecked, cbSettings.isChecked)
                    binding.loadingLayout.loading.visibility = View.GONE
                    binding.loadingLayout.loading.cancelAnimation()
                    Toast.makeText(this@SettingActivity, "Restore Complete. Restarting...", Toast.LENGTH_LONG).show()
                    delay(500)
                    startActivity(Intent.makeRestartActivityTask(packageManager.getLaunchIntentForPackage(packageName)?.component))
                    Runtime.getRuntime().exit(0)
                } else {
                    binding.loadingLayout.loading.visibility = View.GONE
                    binding.loadingLayout.loading.cancelAnimation()
                    Toast.makeText(this@SettingActivity, "Failed to download snapshot", Toast.LENGTH_SHORT).show()
                }
            }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showManageCustomersDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_manage_customers, null)
        bottomSheetDialog.setContentView(view)
        val rvCustomers = view.findViewById<RecyclerView>(R.id.rv_manage_customers)
        val etSearch = view.findViewById<TextInputEditText>(R.id.et_search_customer)
        val btnClearLocal = view.findViewById<MaterialButton>(R.id.btn_clear_all_local)

        rvCustomers.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            val initialCustomers = customerDao.getAllCustomers().first()
            val adapter = ManageCustomerAdapter(initialCustomers) { customer ->
                lifecycleScope.launch {
                    customerDao.markAsDeleted(customer.id)
                    val updated = customerDao.getAllCustomers().first()
                    (rvCustomers.adapter as ManageCustomerAdapter).updateData(updated)
                    // Re-filter if search is active
                    val currentQuery = etSearch.text.toString()
                    if (currentQuery.isNotEmpty()) {
                        (rvCustomers.adapter as ManageCustomerAdapter).filter(currentQuery)
                    }
                    syncManager.pushUnsyncedCustomers()
                }
            }
            rvCustomers.adapter = adapter

            // Search Bar Logic
            etSearch.doAfterTextChanged { text ->
                adapter.filter(text.toString())
            }

            // Task 3: Clear Local Data Logic
            btnClearLocal.setOnClickListener {
                AlertDialog.Builder(this@SettingActivity)
                    .setTitle("Clear Local Storage?")
                    .setMessage("This will remove all synced customers from this phone to save space. Your data on the cloud is perfectly safe and can be restored anytime by pressing 'Force Cloud Sync'.")
                    .setPositiveButton("Clear") { _, _ ->
                        lifecycleScope.launch {
                            customerDao.clearAllSyncedCustomersLocally()
                            val updated = customerDao.getAllCustomers().first()
                            adapter.updateData(updated)
                            Toast.makeText(this@SettingActivity, "Local storage cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        bottomSheetDialog.show()
    }

    private fun showSyncLogs() {
        lifecycleScope.launch {
            val logs = syncLogDao.getLastLogs()
            val logStrings = logs.map { log ->
                val date = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                "[$date] ${log.status}: ${log.details}"
            }
            AlertDialog.Builder(this@SettingActivity).setTitle("Sync History").setItems(logStrings.toTypedArray(), null).setPositiveButton("OK", null).show()
        }
    }

    private fun loadColorView() {
        colorList = loadColors()
        val cardViews = listOf(binding.cv1, binding.cv2, binding.cv3, binding.cv4)
        val views = listOf(binding.view1, binding.view2, binding.view3, binding.view4)

        cardViews.forEachIndexed { index, cardView ->
            cardView.setOnClickListener { updateSelectedCard(views, cardViews, colorList[index]) }
            updateCardBackground(cardView, views[index], index)
        }
        updateSelectedCard(views, cardViews, selectedColorCode)
    }

    private fun showChangeColorDialog(position: Int) {
        val dialogView = EditText(this).apply {
            hint = "Enter hex color (e.g., #FFFFFF)"
            setText(if (position != -1) colorList[position] else "")
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(this).setTitle("Enter Color Code").setView(dialogView).setPositiveButton("Save") { _, _ ->
            val validatedColor = validateAndFormatColor(dialogView.text.toString())
            if (validatedColor != null) {
                selectedColorCode = validatedColor
                colorList[position] = selectedColorCode
                saveColors()
                loadColorView()
            } else Toast.makeText(this, "Invalid color", Toast.LENGTH_SHORT).show()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun validateAndFormatColor(input: String): String? {
        var color = input.trim()
        if (color.isEmpty()) return null
        if (!color.startsWith("#")) color = "#$color"
        return if (color.length == 7 || color.length == 9) (if (isValidColor(color)) color else null) else null
    }

    private fun isValidColor(colorCode: String) = try { Color.parseColor(colorCode); true } catch (e: Exception) { false }

    private fun parseColorSafe(colorCode: String?, defaultColor: String = "#FFFFFF") = try { Color.parseColor(colorCode) } catch (e: Exception) { Color.parseColor(defaultColor) }

    private fun loadColors(): MutableList<String> {
        val savedColors = sharedPreferences.getStringSet("colors", null)
        return if (savedColors.isNullOrEmpty()) mutableListOf("#6750A4", "#3a0ca3", "#FEE440", "#4361ee") else savedColors.toMutableList().also { it.reverse() }
    }

    private fun saveColors() = sharedPreferences.edit().putStringSet("colors", colorList.toSet()).apply()

    private fun updateSelectedCard(views: List<View>, cardViews: List<MaterialCardView>, selectedColor: String) {
        selectedColorCode = selectedColor
        cardViews.forEachIndexed { index, cardView -> updateCardBackground(cardView, views[index], index) }
        sharedPreferences.edit().putString("selected_color_code", selectedColor).apply()
    }

    private fun updateCardBackground(cardView: MaterialCardView, view: View, position: Int) {
        val color = colorList[position]
        val parsedColor = parseColorSafe(color)
        view.setBackgroundColor(parsedColor)
        
        if (color == selectedColorCode) {
            cardView.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics).toInt()
            cardView.strokeColor = ContextCompat.getColor(this, android.R.color.black)
        } else {
            cardView.strokeWidth = 0
        }
    }

    private fun fetchAndDisplayDiscounts() {
        lifecycleScope.launch {
            discountList.clear()
            discountList.addAll(discountDao.getAllDiscountsSorted())
            discountAdapter.notifyDataSetChanged()
        }
    }

    private fun onPerformAction(position: Int, action: DiscountAction) {
        lifecycleScope.launch {
            val discount = discountList[position]
            when (action) {
                DiscountAction.DELETE -> showDeleteConfirmationDialog(position)
                DiscountAction.EDIT -> showEditDialog(position)
                DiscountAction.ACTIVATE -> {
                    discount.isActive = !discount.isActive
                    discount.invoiceId = currentInvoiceId
                    discountDao.update(discount)
                    discountList[position] = discount
                    discountAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        AlertDialog.Builder(this).setTitle("Confirm Deletion").setMessage("Delete this item?").setPositiveButton("Yes") { _, _ ->
            lifecycleScope.launch {
                discountDao.delete(discountList[position])
                discountList.removeAt(position)
                discountAdapter.notifyItemRemoved(position)
            }
        }.setNegativeButton("No", null).show()
    }

    private fun showEditDialog(position: Int) {
        val dialogBinding = EditDiscountDialogBinding.inflate(layoutInflater)
        val item = discountList[position]
        dialogBinding.etTitle.setText(item.title)
        dialogBinding.etPercentage.setText(if (item.percentage > 0) item.percentage.toString() else "")
        dialogBinding.etPrice.setText(if (item.price > 0) item.price.toString() else "")
        
        // Setup initial toggle state
        if (item.isPlus) {
            dialogBinding.rgIsAdd.check(R.id.rb_plus)
        } else {
            dialogBinding.rgIsAdd.check(R.id.rb_minus)
        }
        
        dialogBinding.switchIsActive.isChecked = item.isActive

        dialogBinding.etPercentage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) dialogBinding.etPrice.setText("")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialogBinding.etPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) dialogBinding.etPercentage.setText("")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        AlertDialog.Builder(this).setView(dialogBinding.root).setTitle("Edit Template").setPositiveButton("Save") { _, _ ->
            val title = dialogBinding.etTitle.text.toString().trim()
            if (title.isNotEmpty()) {
                item.title = title
                item.percentage = dialogBinding.etPercentage.text.toString().toIntOrNull() ?: 0
                item.price = dialogBinding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
                item.isPlus = dialogBinding.rgIsAdd.checkedRadioButtonId == R.id.rb_plus
                item.isActive = dialogBinding.switchIsActive.isChecked
                lifecycleScope.launch {
                    discountDao.update(item)
                    fetchAndDisplayDiscounts()
                }
            }
        }.setNegativeButton("Cancel", null).show()
    }
}
