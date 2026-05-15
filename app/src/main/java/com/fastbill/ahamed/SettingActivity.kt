package com.fastbill.ahamed

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.fastbill.ahamed.database.BackupManager
import com.fastbill.ahamed.database.CSVImporter
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.database.InvoiceDatabase
import com.fastbill.ahamed.database.PreferencesRepository
import com.fastbill.ahamed.database.SyncManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingActivity : ComponentActivity() {

    private lateinit var viewModel: SettingsViewModel
    private val database by lazy { InvoiceDatabase.getDatabase(this) }
    private val syncManager by lazy { SyncManager(this) }
    private val backupManager by lazy { BackupManager(this) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val prefsRepo = PreferencesRepository(sharedPreferences)
        val factory = SettingsViewModelFactory(preferencesRepository = prefsRepo, discountDao = database.discountDao())
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF6750A4), background = Color(0xFFF2F4F7))) {
                val state by viewModel.uiState.collectAsState()

                if (!state.isLoaded) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Settings Hub", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                                navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Default.ArrowBack, null) } },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                            )
                        }
                    ) { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF2F4F7)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    SettingsScreen(
                        state = state,
                        viewModel = viewModel,
                        prefsRepo = prefsRepo,
                        database = database,
                        syncManager = syncManager,
                        backupManager = backupManager,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    prefsRepo: PreferencesRepository,
    database: InvoiceDatabase,
    syncManager: SyncManager,
    backupManager: BackupManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var showCustomerSheet by remember { mutableStateOf(false) }
    var showBackupNameDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var availableProfiles by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }

    var showDiscountActionMenu by remember { mutableStateOf<Discount?>(null) }
    var showEditDiscountDialog by remember { mutableStateOf<Discount?>(null) }
    var isGlobalLoading by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    CSVImporter.importCustomersFromCSV(context, it, database.customerDao())
                    Toast.makeText(context, "Import Complete", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) { Toast.makeText(context, "Import Failed", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings Hub", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF2F4F7))
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp)
            ) {

                SectionHeader("BRAND APPEARANCE")
                SettingsGroupCard {
                    var rawColors by remember { mutableStateOf(prefsRepo.getCustomColorListString()) }
                    val colorList = rawColors.split(",").take(4).toMutableList()
                    var selectedIndex by remember { mutableStateOf(colorList.indexOf(state.selectedColor).takeIf { it >= 0 } ?: 0) }
                    var hexInput by remember(state.selectedColor) { mutableStateOf(state.selectedColor) }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        colorList.forEachIndexed { index, colorStr ->
                            val color = try { Color(android.graphics.Color.parseColor(colorStr)) } catch (e: Exception) { Color.Gray }
                            val isSelected = selectedIndex == index
                            Box(
                                modifier = Modifier.size(50.dp).clip(CircleShape).background(color)
                                    .border(if (isSelected) 3.dp else 0.dp, Color.Black.copy(alpha=0.2f), CircleShape)
                                    .clickable {
                                        selectedIndex = index; hexInput = colorStr; prefsRepo.setSelectedColor(colorStr)
                                        viewModel.refreshSettings(); focusManager.clearFocus()
                                    },
                                contentAlignment = Alignment.Center) { if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                        }
                    }
                    DividerRow()
                    InlineInputRow(
                        label = "Custom Hex Code",
                        value = hexInput,
                        onValueChange = { hexInput = it },
                        trailingContent = {
                            IconButton(onClick = {
                                if (hexInput.length == 7 && hexInput.startsWith("#")) {
                                    try {
                                        android.graphics.Color.parseColor(hexInput)
                                        colorList[selectedIndex] = hexInput
                                        val newColorString = colorList.joinToString(",")
                                        prefsRepo.setCustomColorListString(newColorString)
                                        rawColors = newColorString; prefsRepo.setSelectedColor(hexInput)
                                        viewModel.refreshSettings(); focusManager.clearFocus()
                                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) { Toast.makeText(context, "Invalid Hex", Toast.LENGTH_SHORT).show() }
                                }
                            }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                        }
                    )
                }

                SectionHeader("BILL DESIGN TEMPLATE")
                SettingsGroupCard {
                    var selectedDesign by remember { mutableStateOf(prefsRepo.getBillDesign()) }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp), 
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Modern" to "Modern (Paytm)", 
                            "Classic" to "Classic (Legacy)",
                            "Premium" to "Premium (SaaS)",
                            "Standard" to "Standard (Ledger)"
                        ).forEach { (code, label) ->
                            val isSel = selectedDesign == code
                            Surface(
                                color = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFFF2F4F7),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.widthIn(min = 120.dp).clickable {
                                    selectedDesign = code
                                    prefsRepo.setBillDesign(code)
                                    viewModel.refreshSettings()
                                    focusManager.clearFocus()
                                }
                            ) {
                                Text(label, color = if (isSel) Color.White else Color.DarkGray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                SectionHeader("INVOICE DEFAULTS")
                SettingsGroupCard {
                    var qtyText by remember(state.defaultQuantity) { mutableStateOf(state.defaultQuantity.toString()) }
                    InlineInputRow(
                        label = "Default Quantity",
                        value = qtyText,
                        onValueChange = {
                            qtyText = it
                            it.toIntOrNull()?.let { qty -> if (qty > 0) { prefsRepo.setDefaultQuantity(qty); viewModel.refreshSettings() } }
                        },
                        keyboardType = KeyboardType.NumberPassword
                    )
                }

                SectionHeader("WHATSAPP & SHARING")
                SettingsGroupCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Preferred App", fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("wa" to "WA", "wa_biz" to "WA Business", "other" to "Other").forEach { (code, label) ->
                                val isSel = state.shareApp == code
                                Surface(
                                    color = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFFF2F4F7),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).clickable { prefsRepo.setShareAppPreference(code); viewModel.refreshSettings(); focusManager.clearFocus() }
                                ) {
                                    Text(label, color = if (isSel) Color.White else Color.DarkGray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    DividerRow()

                    SettingsToggleRow("Include Text Caption", state.isCaptionOn) { prefsRepo.setShareCaptionOn(it); viewModel.refreshSettings(); focusManager.clearFocus() }

                    AnimatedVisibility(visible = state.isCaptionOn) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            var capText by remember(state.captionTemplate) { mutableStateOf(state.captionTemplate) }
                            OutlinedTextField(
                                value = capText,
                                onValueChange = { capText = it; prefsRepo.setShareCaptionTemplate(it); viewModel.refreshSettings() },
                                label = { Text("Caption Template") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3,
                                maxLines = 6
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    DividerRow()

                    SettingsToggleRow("Direct Number Posting", state.isNumberOn) { prefsRepo.setShareNumberOn(it); viewModel.refreshSettings(); focusManager.clearFocus() }
                    AnimatedVisibility(visible = state.isNumberOn) {
                        Column {
                            var n1 by remember(state.number1) { mutableStateOf(state.number1) }
                            var n2 by remember(state.number2) { mutableStateOf(state.number2) }
                            var n3 by remember(state.number3) { mutableStateOf(state.number3) }

                            InlineInputRow("Primary Number", n1, onValueChange = { n1 = it; prefsRepo.setShareNumber(1, it); viewModel.refreshSettings() }, keyboardType = KeyboardType.Phone)
                            DividerRow()
                            InlineInputRow("Secondary Number", n2, onValueChange = { n2 = it; prefsRepo.setShareNumber(2, it); viewModel.refreshSettings() }, keyboardType = KeyboardType.Phone)
                            DividerRow()
                            InlineInputRow("Tertiary Number", n3, onValueChange = { n3 = it; prefsRepo.setShareNumber(3, it); viewModel.refreshSettings() }, keyboardType = KeyboardType.Phone)
                        }
                    }
                }

                SectionHeader("DATA, SYNC & CLOUD")
                SettingsGroupCard {
                    SettingsActionRow(Icons.Default.Group, Color(0xFF2196F3), "Manage Customers", onClick = { focusManager.clearFocus(); showCustomerSheet = true })
                    DividerRow()
                    SettingsActionRow(Icons.Default.UploadFile, Color(0xFF009688), "Import Customers (CSV)", onClick = { focusManager.clearFocus(); csvLauncher.launch("text/comma-separated-values") })
                    DividerRow()

                    var backupDays by remember(state.backupDays) { mutableStateOf(state.backupDays.toString()) }
                    InlineInputRow("Auto-Backup Interval", backupDays, onValueChange = { 
                        backupDays = it
                        it.toIntOrNull()?.let { days -> prefsRepo.setAutoBackupDays(days); viewModel.refreshSettings() } 
                    }, keyboardType = KeyboardType.NumberPassword)
                    DividerRow()

                    SettingsActionRow(Icons.Default.CloudUpload, MaterialTheme.colorScheme.primary, "Create Backup Profile", onClick = {
                        focusManager.clearFocus()
                        val name = prefsRepo.getDefaultBackupName()
                        if (name.isNullOrBlank()) showBackupNameDialog = true
                        else {
                            isGlobalLoading = true
                            coroutineScope.launch { backupManager.createAndUploadSnapshot(name); isGlobalLoading = false; Toast.makeText(context, "Backup Success", Toast.LENGTH_SHORT).show() }
                        }
                    })
                    DividerRow()
                    SettingsActionRow(Icons.Default.CloudDownload, Color(0xFFFF9800), "Restore Profile",
                        onClick = {
                            focusManager.clearFocus(); isGlobalLoading = true
                            coroutineScope.launch {
                                backupManager.getAvailableProfilesWithDates().onSuccess { profiles -> availableProfiles = profiles; showRestoreDialog = true }
                                    .onFailure { Toast.makeText(context, "No backups found", Toast.LENGTH_SHORT).show() }
                                isGlobalLoading = false
                            }
                        },
                        onLongClick = { focusManager.clearFocus(); showLogsDialog = true }
                    )
                    DividerRow()
                    SettingsActionRow(Icons.Default.Sync, Color(0xFF673AB7), "Force Cloud Sync",
                        onClick = {
                            focusManager.clearFocus(); isGlobalLoading = true
                            coroutineScope.launch { syncManager.pushUnsyncedCustomers(); isGlobalLoading = false; Toast.makeText(context, "Sync Done", Toast.LENGTH_SHORT).show() }
                        },
                        onLongClick = { focusManager.clearFocus(); showLogsDialog = true }
                    )
                    DividerRow()
                    SettingsActionRow(Icons.Default.Edit, Color.Gray, "Change Backup Name", value = prefsRepo.getDefaultBackupName() ?: "Default", onClick = { focusManager.clearFocus(); showBackupNameDialog = true })
                }

                SectionHeader("DISCOUNT & TAX TEMPLATES")
                SettingsGroupCard {
                    var newTitle by remember { mutableStateOf("") }
                    var newPct by remember { mutableStateOf("") }
                    var newPrice by remember { mutableStateOf("") }
                    var isPlus by remember { mutableStateOf(false) }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Create New Template", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                        OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, placeholder = { Text("Name (e.g. GST, Wholesale)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = newPct, onValueChange = { newPct = it }, placeholder = { Text("%") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
                            OutlinedTextField(value = newPrice, onValueChange = { editPrice -> newPrice = editPrice }, placeholder = { Text("Fixed ₹") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(selected = !isPlus, onClick = { isPlus = false }, label = { Text("Discount (-)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }, modifier = Modifier.weight(1f))
                            FilterChip(selected = isPlus, onClick = { isPlus = true }, label = { Text("Charge (+)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }, modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                if(newTitle.isNotBlank()) {
                                    coroutineScope.launch {
                                        database.discountDao().insert(Discount(title = newTitle, percentage = newPct.toIntOrNull() ?: 0, price = newPrice.toDoubleOrNull() ?: 0.0, isPlus = isPlus, isActive = true))
                                        viewModel.refreshDiscounts(); newTitle = ""; newPct = ""; newPrice = ""; focusManager.clearFocus()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("SAVE TEMPLATE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                    }
                }

                if (state.discounts.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    SettingsGroupCard {
                        state.discounts.forEachIndexed { index, discount ->
                            key(discount.id) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().combinedClickable(
                                        onClick = { coroutineScope.launch { database.discountDao().update(discount.copy(isActive = !discount.isActive)); viewModel.refreshDiscounts() } },
                                        onLongClick = { showDiscountActionMenu = discount }
                                    ).padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(discount.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val amt = if(discount.percentage > 0) "${discount.percentage}%" else "₹${discount.price}"
                                        Text(if (discount.isPlus) "Charge: +$amt" else "Discount: -$amt", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Switch(checked = discount.isActive, onCheckedChange = { coroutineScope.launch { database.discountDao().update(discount.copy(isActive = it)); viewModel.refreshDiscounts() } })
                                }
                                if (index < state.discounts.size - 1) DividerRow()
                            }
                        }
                    }
                }

                AppVersionFooter()
            }

            if (isGlobalLoading) { Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) } }
        }
    }

    // --- DIALOGS & BOTTOM SHEETS ---
    if (showCustomerSheet) ManageCustomersSheet(onDismiss = { showCustomerSheet = false }, database = database)

    if (showBackupNameDialog) {
        var tempName by remember { mutableStateOf(prefsRepo.getDefaultBackupName() ?: "") }
        AlertDialog(
            onDismissRequest = { showBackupNameDialog = false },
            title = { Text("Backup Profile Name", fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = tempName, onValueChange = { tempName = it }, label = { Text("Business Name") }, shape = RoundedCornerShape(12.dp)) },
            confirmButton = { TextButton(onClick = { prefsRepo.setDefaultBackupName(tempName); showBackupNameDialog = false }) { Text("Save", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showBackupNameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showRestoreDialog && availableProfiles.isNotEmpty()) {
        var selectedProfile by remember { mutableStateOf(availableProfiles.first().first) }
        var restoreBills by remember { mutableStateOf(true) }
        var restoreSettings by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Select Backup to Restore", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    availableProfiles.forEach { profile ->
                        val timestamp = if (profile.second < 10000000000L) profile.second * 1000L else profile.second
                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedProfile = profile.first }.padding(vertical = 4.dp)) {
                            RadioButton(selected = selectedProfile == profile.first, onClick = { selectedProfile = profile.first })
                            Text("${profile.first} ($dateStr)", fontSize = 14.sp)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreBills, onCheckedChange = { restoreBills = it })
                        Text("Restore Bills Database", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreSettings, onCheckedChange = { restoreSettings = it })
                        Text("Restore App Settings", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    isGlobalLoading = true
                    showRestoreDialog = false
                    coroutineScope.launch {
                        val snapshot = backupManager.downloadSnapshot(selectedProfile)
                        if (snapshot != null) {
                            backupManager.applySelectiveRestore(snapshot, restoreBills, restoreSettings)
                            Toast.makeText(context, "Restore Complete! Restarting...", Toast.LENGTH_LONG).show()
                            val restartIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            restartIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(restartIntent)
                            Runtime.getRuntime().exit(0)
                        } else {
                            isGlobalLoading = false
                            Toast.makeText(context, "Failed to download backup", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Restore & Restart", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") } }
        )
    }

    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = { Text("System Sync Logs", fontWeight = FontWeight.Bold) },
            text = { LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) { item { Text("Logs available in System Settings", fontSize = 12.sp, fontFamily = FontFamily.Monospace) } } },
            confirmButton = { TextButton(onClick = { showLogsDialog = false }) { Text("Close") } }
        )
    }

    showDiscountActionMenu?.let { discount ->
        AlertDialog(
            onDismissRequest = { showDiscountActionMenu = null },
            title = { Text("Manage Template", fontWeight = FontWeight.Bold) },
            text = { Text("What would you like to do with '${discount.title}'?") },
            confirmButton = { TextButton(onClick = { showDiscountActionMenu = null; showEditDiscountDialog = discount }) { Text("Edit") } },
            dismissButton = { TextButton(onClick = { showDiscountActionMenu = null; coroutineScope.launch { database.discountDao().delete(discount); viewModel.refreshDiscounts() }}) { Text("Delete", color = Color.Red) } }
        )
    }

    showEditDiscountDialog?.let { discount ->
        var editTitle by remember { mutableStateOf(discount.title) }
        var editPct by remember { mutableStateOf(if(discount.percentage > 0) discount.percentage.toString() else "") }
        var editPrice by remember { mutableStateOf(if(discount.price > 0) discount.price.toString() else "") }
        var editIsPlus by remember { mutableStateOf(discount.isPlus) }

        AlertDialog(
            onDismissRequest = { showEditDiscountDialog = null },
            title = { Text("Edit Template", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = editPct, onValueChange = { editPct = it }, label = { Text("%") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(value = editPrice, onValueChange = { editPrice = it }, label = { Text("Fixed ₹") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), shape = RoundedCornerShape(12.dp))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !editIsPlus, onClick = { editIsPlus = false }, label = { Text("Discount (-)") }, modifier = Modifier.weight(1f))
                        FilterChip(selected = editIsPlus, onClick = { editIsPlus = true }, label = { Text("Charge (+)") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        database.discountDao().update(discount.copy(title = editTitle, percentage = editPct.toIntOrNull() ?: 0, price = editPrice.toDoubleOrNull() ?: 0.0, isPlus = editIsPlus))
                        viewModel.refreshDiscounts(); showEditDiscountDialog = null
                    }
                }) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showEditDiscountDialog = null }) { Text("Cancel") } }
        )
    }
}

// --- REUSABLE SAAS UI ATOMS ---

@Composable
fun SectionHeader(title: String) {
    Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color(0xFF8B95A5), modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp))
}

@Composable
fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(content = content)
    }
}

@Composable
fun DividerRow() {
    HorizontalDivider(color = Color(0xFFF2F4F7), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun InlineInputRow(label: String, value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text, trailingContent: (@Composable () -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        BasicTextField(
            value = value, onValueChange = onValueChange, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), textStyle = TextStyle(fontSize = 15.sp, color = Color.Black, textAlign = TextAlign.End), singleLine = true, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFFF2F4F7), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp).widthIn(min = 60.dp, max = 180.dp)) {
                    Box(Modifier.weight(1f)) { innerTextField() }
                    trailingContent?.invoke()
                }
            }
        )
    }
}

@Composable
fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String, value: String? = null, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
        Text(text, modifier = Modifier.weight(1f).padding(start = 16.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        if (value != null) Text(value, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCustomersSheet(onDismiss: () -> Unit, database: InvoiceDatabase) {
    val customers by database.customerDao().getAllCustomers().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).fillMaxHeight(0.85f)) {
            Text("Manage Customers", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Customers") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Button(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) { Text("CLEAR LOCAL DATABASE", fontWeight = FontWeight.Bold) }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text("Clear All Customers?") },
                    text = { Text("This will permanently delete all local customer data. This cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                database.customerDao().clearAllSyncedCustomersLocally()
                                showClearConfirm = false
                                onDismiss()
                            }
                        }) { Text("CLEAR", color = Color.Red, fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) { Text("CANCEL") }
                    }
                )
            }

            val filteredList = if (searchQuery.isBlank()) customers else customers.filter { it.customerName.contains(searchQuery, ignoreCase = true) }

            LazyColumn {
                items(filteredList) { c ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.customerName, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        IconButton(onClick = { coroutineScope.launch { database.customerDao().markAsDeleted(c.id) } }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    }
                }
            }
        }
    }
}

@Composable
fun AppVersionFooter() {
    val context = LocalContext.current
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) { null }
    }

    val versionName = packageInfo?.versionName ?: "Unknown"
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "0"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "0"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FASTBILL ENGINE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color(0xFF9CA3AF)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = Color(0xFFE5E7EB),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Version $versionName • Build $versionCode",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
