package com.fastbill.ahamed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.fastbill.ahamed.R
import com.fastbill.ahamed.adapter.ItemDiscountSettingAdapter
import com.fastbill.ahamed.database.CSVImporter
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.database.InvoiceDatabase
import com.fastbill.ahamed.database.SyncManager
import com.fastbill.ahamed.databinding.ActivitySettingBinding
import com.fastbill.ahamed.databinding.EditDiscountDialogBinding
import com.fastbill.ahamed.model.DiscountAction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingBinding
    private lateinit var discountAdapter: ItemDiscountSettingAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var discountList: MutableList<Discount>
    private lateinit var colorList: MutableList<String>
    private var selectedColorCode = "#6750A4"
    private var currentInvoiceId: Int? = null
    private val database by lazy {
        InvoiceDatabase.getDatabase(this)
    }
    private val discountDao by lazy {
        database.discountDao()
    }
    private val customerDao by lazy {
        database.customerDao()
    }
    private val syncLogDao by lazy {
        database.syncLogDao()
    }
    private val syncManager by lazy {
        SyncManager(this)
    }

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
            // Include both system bars and the soft keyboard (ime) to prevent occlusion
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.imgBack.setOnClickListener {
            finish()
        }

        currentInvoiceId = intent.getIntExtra("invoiceId", -1)
        if (currentInvoiceId == -1) {
            currentInvoiceId = null // No invoiceId provided
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Load Share Settings
        val shareApp = sharedPreferences.getString("share_app", "other")
        when (shareApp) {
            "wa" -> binding.rgShareApp.check(R.id.rb_wa)
            "wa_biz" -> binding.rgShareApp.check(R.id.rb_wa_biz)
            else -> binding.rgShareApp.check(R.id.rb_other)
        }
        
        binding.switchCaption.isChecked = sharedPreferences.getBoolean("share_caption", true)
        binding.etCaptionTemplate.setText(sharedPreferences.getString("share_caption_template", "*Total {qty} Pcs {total}*"))
        binding.etCaptionTemplate.visibility = if (binding.switchCaption.isChecked) View.VISIBLE else View.GONE
        
        binding.switchCaption.setOnCheckedChangeListener { _, isChecked ->
            binding.etCaptionTemplate.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        val isNumberOn = sharedPreferences.getBoolean("share_number_on", false)
        binding.switchNumber.isChecked = isNumberOn
        binding.etDefaultNumber.visibility = if (isNumberOn) android.view.View.VISIBLE else android.view.View.GONE
        binding.etDefaultNumber.setText(sharedPreferences.getString("share_number_val", ""))

        // Toggle EditText visibility dynamically
        binding.switchNumber.setOnCheckedChangeListener { _, isChecked ->
            binding.etDefaultNumber.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
        }

        val defaultQuantity = sharedPreferences.getInt("default_quantity", 4)
        binding.quantityInput.setText("$defaultQuantity")
        binding.btnUpdate.setOnClickListener {
            val quantity = binding.quantityInput.text.toString().toIntOrNull() ?: 0
            if (quantity > 0) {
                val editor = sharedPreferences.edit()
                editor.putInt("default_quantity", quantity)

                // Save Share Settings
                val selectedShareApp = when (binding.rgShareApp.checkedRadioButtonId) {
                    R.id.rb_wa -> "wa"
                    R.id.rb_wa_biz -> "wa_biz"
                    else -> "other"
                }
                editor.putString("share_app", selectedShareApp)
                editor.putBoolean("share_caption", binding.switchCaption.isChecked)
                editor.putString("share_caption_template", binding.etCaptionTemplate.text.toString().trim())
                editor.putBoolean("share_number_on", binding.switchNumber.isChecked)
                editor.putString("share_number_val", binding.etDefaultNumber.text.toString())

                editor.apply()
                val inputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.quantityInput.windowToken, 0)
                binding.quantityInput.clearFocus()
                Toast.makeText(this, "Settings updated.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Quantity should > 0", Toast.LENGTH_SHORT).show()
            }
        }
        binding.quantityInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnUpdate.performClick()
                true // Consume the event
            } else {
                false // Let the system handle other actions
            }
        }

        // Add TextWatcher to etPercentage
        binding.etPercentage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    binding.etPrice.setText("") // Clear price when percentage is typed
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Add TextWatcher to etPrice
        binding.etPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    binding.etPercentage.setText("") // Clear percentage when price is typed
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Set default states for new discount input
        binding.rgIsAdd.check(R.id.rb_plus)
        binding.switchIsActive.isChecked = true

        binding.btnAdd.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val percentageInput = binding.etPercentage.text.toString().trim()
            val priceInput = binding.etPrice.text.toString().trim()

            // Validate inputs
            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (percentageInput.isEmpty() && priceInput.isEmpty()) {
                Toast.makeText(this, "Please enter either percentage or price", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Parse percentage and price
            val percentage = if (percentageInput.isNotEmpty()) percentageInput.toIntOrNull() else 0
            val price = if (priceInput.isNotEmpty()) priceInput.toDoubleOrNull() ?: 0.0 else 0.0

            // Reading state from RadioGroup and SwitchCompat
            val isPlus = binding.rgIsAdd.checkedRadioButtonId == R.id.rb_plus
            val isActive = binding.switchIsActive.isChecked

            // Create a Discount object
            val discount = Discount(
                title = title,
                percentage = percentage ?: 0,
                price = price,
                isPlus = isPlus,
                isActive = isActive,
                orderIndex = discountList.size
            )

            // Insert the discount into the database
            lifecycleScope.launch {
                discountDao.insert(discount)
                Toast.makeText(
                    this@SettingActivity, "Record added successfully", Toast.LENGTH_SHORT
                ).show()
                fetchAndDisplayDiscounts()
                // Clear input fields after insertion
                binding.etTitle.text.clear()
                binding.etPercentage.text.clear()
                binding.etPrice.text.clear()
                binding.etTitle.requestFocus()
            }
        }

        discountList = mutableListOf()
        discountAdapter = ItemDiscountSettingAdapter(discountList, ::onPerformAction)
        binding.rvDiscount.layoutManager =
            GridLayoutManager(this, 2)
        binding.rvDiscount.adapter = discountAdapter
        fetchAndDisplayDiscounts()

        selectedColorCode =
            sharedPreferences.getString("selected_color_code", "#6750A4") ?: "#6750A4"

        loadColorView()
        binding.btnChange.setOnClickListener {
            showChangeColorDialog(colorList.indexOf(selectedColorCode))
        }

        binding.btnImportCsv.setOnClickListener {
            csvPickerLauncher.launch("text/comma-separated-values")
        }

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

        binding.btnForceSync.setOnLongClickListener {
            showSyncLogs()
            true
        }
    }

    private fun showSyncLogs() {
        lifecycleScope.launch {
            val logs = syncLogDao.getLastLogs()
            val logStrings = logs.map { log ->
                val date = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                "[$date] ${log.status}: ${log.details}"
            }
            
            AlertDialog.Builder(this@SettingActivity)
                .setTitle("Sync History (Last 20)")
                .setItems(logStrings.toTypedArray(), null)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun loadColorView() {
        colorList = loadColors()
        val cardViews = listOf(
            binding.cv1, binding.cv2, binding.cv3, binding.cv4
        )
        val views = listOf(
            binding.view1, binding.view2, binding.view3, binding.view4
        )

// Set up click listeners and initial colors
        cardViews.forEachIndexed { index, cardView ->
            cardView.setOnClickListener {
                // Update the selected card with the corresponding color
                updateSelectedCard(views, cardViews, colorList[index])
            }
            // Initialize the background for each card
            updateCardBackground(cardView, views[index], index)
        }

// Select the card with the saved color by default
        updateSelectedCard(views, cardViews, selectedColorCode)
    }

    private var colorDialog: AlertDialog? = null
    private fun showChangeColorDialog(position: Int) {
        val dialogView = EditText(this).apply {
            hint = "Enter valid color code (e.g., #FFFFFF)"
            setText(if (position != -1) colorList[position] else "")
            // Set IME options to "Done"
            imeOptions = EditorInfo.IME_ACTION_DONE

            // Set input type to text for color codes
            inputType = InputType.TYPE_CLASS_TEXT
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val newColor = text.toString().trim()
                    if (isValidColor(newColor)) {
                        selectedColorCode = newColor
                        colorList[position] = selectedColorCode
                        saveColors()
                        loadColorView()
                        colorDialog?.dismiss() // Dismiss the dialog after saving
                    } else {
                        Toast.makeText(context, "Invalid color code", Toast.LENGTH_SHORT).show()
                    }
                    true // Consume the event
                } else {
                    false // Let the system handle other actions
                }
            }
        }

        colorDialog = AlertDialog.Builder(this).setTitle("Enter Color Code").setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newColor = dialogView.text.toString().trim()
                if (isValidColor(newColor)) {
                    selectedColorCode = newColor
                    colorList[position] = selectedColorCode
                    saveColors()
                    loadColorView() // Update UI dynamically
                } else {
                    Toast.makeText(this, "Invalid color code", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Cancel", null).create()

//        colorDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        colorDialog?.show()
    }

    private fun isValidColor(colorCode: String): Boolean {
        return try {
            Color.parseColor(colorCode)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun loadColors(): MutableList<String> {
        val savedColors = sharedPreferences.getStringSet("colors", null)
        return if (savedColors.isNullOrEmpty()) {
            mutableListOf("#6750A4", "#3a0ca3", "#FEE440", "#4361ee")
        } else {
            val colorList = savedColors.toMutableList()
            colorList.reverse() // Reverse the list in place
            colorList
        }
    }

    private fun saveColors() {
        val editor = sharedPreferences.edit()
        editor.putStringSet("colors", colorList.toSet())
        editor.apply()
    }

    private fun updateSelectedCard(
        views: List<View>, cardViews: List<CardView>, selectedColor: String
    ) {
        // Update the selected color code
        selectedColorCode = selectedColor

        // Update the background for all cards
        cardViews.forEachIndexed { index, cardView ->
            updateCardBackground(cardView, views[index], index)
        }

        // Save the selected color code to SharedPreferences
        sharedPreferences.edit().putString("selected_color_code", selectedColor).apply()
    }

    private fun updateCardBackground(cardView: CardView, view: View, position: Int) {
        val color = colorList[position]
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f // Set corner radius
            setColor(Color.parseColor(color)) // Set background color
        }

        // Set the card's background color
        cardView.setCardBackgroundColor(
            if (color == selectedColorCode) Color.parseColor("#FF0000") else Color.parseColor(color)
        )

        // Apply the drawable to the view inside the card
        view.background = drawable
    }

    private fun fetchAndDisplayDiscounts() {
        lifecycleScope.launch {
            discountList.clear()
            val activeList = discountDao.getAllDiscountsSorted()
            discountList.addAll(activeList)
            discountAdapter.notifyDataSetChanged()
            Log.e("check_size", "Size: ${discountList.size}")
        }
    }

    private fun onPerformAction(position: Int, action: DiscountAction) {
        lifecycleScope.launch {
            val discount = discountList[position] // Get the item at the specified position

            when (action) {
                DiscountAction.DELETE -> {
                    showDeleteConfirmationDialog(position)
                }

                DiscountAction.EDIT -> {
                    showEditDialog(position)
                }

                DiscountAction.ACTIVATE -> {
                    discount.isActive = !discount.isActive
//                    if (discount.isActive) {
                        discount.invoiceId = currentInvoiceId // Link to the current invoice
//                    } else {
//                        discount.invoiceId = null // Remove link
//                    }
                    discountDao.update(discount) // Persist changes to the database
                    discountList[position] = discount
                    discountAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Deletion")
        builder.setMessage("Are you sure you want to delete this item?")
        builder.setPositiveButton("Yes") { _, _ ->
            lifecycleScope.launch {
                val discount = discountList[position]
                discountDao.delete(discount)
                discountList.removeAt(position)
                discountAdapter.notifyItemRemoved(position)
            }
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss() // Dismiss the dialog if the user cancels
        }
        builder.create().show()
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

        // Setting state from currentItem
        if (currentItem.isPlus) {
            dialogView.rgIsAdd.check(R.id.rb_plus)
        } else {
            dialogView.rgIsAdd.check(R.id.rb_minus)
        }
        dialogView.switchIsActive.isChecked = currentItem.isActive

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

                if (title.isEmpty()) {
                    Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (percentageInput.isEmpty() && priceInput.isEmpty()) {
                    Toast.makeText(
                        this, "Please enter either percentage or price", Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                // Parse percentage and price
                val percentage =
                    if (percentageInput.isNotEmpty()) percentageInput.toIntOrNull() else 0
                val price = if (priceInput.isNotEmpty()) priceInput.toDoubleOrNull() ?: 0.0 else 0.0

                // Reading state from RadioGroup and SwitchCompat
                val isPlus = dialogView.rgIsAdd.checkedRadioButtonId == R.id.rb_plus
                val isActive = dialogView.switchIsActive.isChecked

                discountList[position].title = title
                discountList[position].percentage = percentage ?: 0
                discountList[position].price = price
                discountList[position].isPlus = isPlus
                discountList[position].isActive = isActive
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
