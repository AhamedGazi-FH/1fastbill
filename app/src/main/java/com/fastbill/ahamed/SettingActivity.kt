package com.fastbill.ahamed

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.fastbill.ahamed.database.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
        val factory = InvoiceViewModelFactory(preferencesRepository = prefsRepo, discountDao = database.discountDao())
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF6750A4), background = Color(0xFFF2F4F7))) {
                val state by viewModel.uiState.collectAsState()

                if (!state.isLoaded) {
                    // SKELETON RENDER (Instant)
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
                    SettingsScreen(state, prefsRepo, sharedPreferences)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    fun SettingsScreen(state: SettingsUiState, prefsRepo: PreferencesRepository, sharedPreferences: android.content.SharedPreferences) {
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

        // 🧠 BIG TECH FIX: Enterprise Staggered Rendering (Incremental Composition)
        var renderState by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            while (renderState < 5) {
                withFrameNanos { }
                renderState++
            }
        }

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
                    navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Default.ArrowBack, null) } },
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
                        .padding(bottom = 40.dp)
                ) {

                    // --- CHUNK 1: APPEARANCE (Loads instantly on Frame 1) ---
                    if (renderState >= 1) {
                        SectionHeader("BRAND APPEARANCE")
                        SettingsGroupCard {
                            val defaultColors = "#6750A4,#FFC107,#3F51B5,#4CAF50"
                            var rawColors by remember { mutableStateOf(sharedPreferences.getString("custom_color_list", defaultColors) ?: defaultColors) }
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
                                        contentAlignment = Alignment.Center
                                    ) { if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
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
                                                sharedPreferences.edit().putString("custom_color_list", newColorString).apply()
                                                rawColors = newColorString; prefsRepo.setSelectedColor(hexInput)
                                                viewModel.refreshSettings(); focusManager.clearFocus()
                                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) { Toast.makeText(context, "Invalid Hex", Toast.LENGTH_SHORT).show() }
                                        }
                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                }
                            )
                        }
                    }

                    // --- CHUNK 2: DEFAULTS & WHATSAPP (Loads sequentially on Frame 2) ---
                    if (renderState >= 2) {
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
                                keyboardType = KeyboardType.Number
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
                    }

                    // --- CHUNK 3: DATA & SYNC (Loads sequentially on Frame 3) ---
                    if (renderState >= 3) {
                        SectionHeader("DATA, SYNC & CLOUD")
                        SettingsGroupCard {
                            SettingsActionRow(Icons.Default.Group, Color(0xFF2196F3), "Manage Customers", onClick = { focusManager.clearFocus(); showCustomerSheet = true })
                            DividerRow()
                            SettingsActionRow(Icons.Default.UploadFile, Color(0xFF009688), "Import Customers (CSV)", onClick = { focusManager.clearFocus(); csvLauncher.launch("text/comma-separated-values") })
                            DividerRow()

                            var backupDays by remember { mutableStateOf(sharedPreferences.getInt("auto_backup_days", 5).toString()) }
                            InlineInputRow("Auto-Backup Interval", backupDays, onValueChange = { backupDays = it; it.toIntOrNull()?.let { days -> sharedPreferences.edit().putInt("auto_backup_days", days).apply() } }, keyboardType = KeyboardType.Number)
                            DividerRow()

                            SettingsActionRow(Icons.Default.CloudUpload, MaterialTheme.colorScheme.primary, "Create Backup Profile", onClick = {
                                focusManager.clearFocus()
                                val name = sharedPreferences.getString("default_backup_name", "")
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
                            SettingsActionRow(Icons.Default.Edit, Color.Gray, "Change Backup Name", value = sharedPreferences.getString("default_backup_name", "Default"), onClick = { focusManager.clearFocus(); showBackupNameDialog = true })
                        }
                    }

                    // --- CHUNK 4: TEMPLATES FORM (Loads sequentially on Frame 4) ---
                    if (renderState >= 4) {
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
                                    OutlinedTextField(value = newPct, onValueChange = { newPct = it }, placeholder = { Text("%") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                    OutlinedTextField(value = newPrice, onValueChange = { newPrice = it }, placeholder = { Text("Fixed ₹") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
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
                    }

                    // --- CHUNK 5: TEMPLATE LIST (Loads sequentially on Frame 5) ---
                    if (renderState >= 5 && state.discounts.isNotEmpty()) {
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
                }

                if (isGlobalLoading) { Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) } }
            }
        }

        // --- DIALOGS & BOTTOM SHEETS ---
        if (showCustomerSheet) ManageCustomersSheet(onDismiss = { showCustomerSheet = false }, database = database)

        if (showBackupNameDialog) {
            var tempName by remember { mutableStateOf(sharedPreferences.getString("default_backup_name", "") ?: "") }
            AlertDialog(
                onDismissRequest = { showBackupNameDialog = false },
                title = { Text("Backup Profile Name", fontWeight = FontWeight.Bold) },
                text = { OutlinedTextField(value = tempName, onValueChange = { tempName = it }, label = { Text("Business Name") }, shape = RoundedCornerShape(12.dp)) },
                confirmButton = { TextButton(onClick = { sharedPreferences.edit().putString("default_backup_name", tempName).apply(); showBackupNameDialog = false }) { Text("Save", fontWeight = FontWeight.Bold) } },
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
                            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(profile.second))
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
            val logs = sharedPreferences.getString("sync_logs", "No recent logs available.") ?: "No logs available."
            AlertDialog(
                onDismissRequest = { showLogsDialog = false },
                title = { Text("System Sync Logs", fontWeight = FontWeight.Bold) },
                text = { LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) { item { Text(logs, fontSize = 12.sp, fontFamily = FontFamily.Monospace) } } },
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
                            OutlinedTextField(value = editPct, onValueChange = { editPct = it }, label = { Text("%") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = editPrice, onValueChange = { editPrice = it }, label = { Text("Fixed ₹") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
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
                    onClick = { coroutineScope.launch { database.customerDao().clearAllSyncedCustomersLocally(); onDismiss() } },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("CLEAR LOCAL DATABASE", fontWeight = FontWeight.Bold) }

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
}