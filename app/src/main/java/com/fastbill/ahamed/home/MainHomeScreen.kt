package com.fastbill.ahamed.home

import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fastbill.ahamed.InvoiceActivity
import com.fastbill.ahamed.InvoiceViewModel
import com.fastbill.ahamed.R
import com.fastbill.ahamed.SettingActivity
import com.fastbill.ahamed.database.CustomerDao
import com.fastbill.ahamed.database.InvoiceDatabase
import com.fastbill.ahamed.database.Rate
import com.fastbill.ahamed.database.RateDao
import com.fastbill.ahamed.model.InvoiceUiState
import com.fastbill.ahamed.model.TemporaryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val homeFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainHomeScreen(
    viewModel: InvoiceViewModel,
    state: InvoiceUiState,
    customerDao: CustomerDao,
    rateDao: RateDao,
    database: InvoiceDatabase,
    sharedPreferences: SharedPreferences,
    onShareInvoice: () -> Unit,
    onShareFirebase: () -> Unit,
    onSaveInvoice: () -> Unit,
    onResetApp: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // 🚀 FIX #10: Handle NaN/Infinity safely during formatting
    fun fmt(amt: Double): String = if (amt.isNaN() || amt.isInfinite()) "0" else homeFormat.format(amt.roundToInt())

    val latoFamily = FontFamily(Font(R.font.lato_regular, FontWeight.Normal), Font(R.font.lato_bold, FontWeight.Bold))
    val dateString = remember { SimpleDateFormat("dd-MMM-yy", Locale.getDefault()).format(Date()) }

    var showQuickSetup by remember { mutableStateOf(false) }
    var showQuickShare by remember { mutableStateOf(false) }
    var editItemIndex by remember { mutableStateOf<Int?>(null) }
    var editDiscountIndex by remember { mutableStateOf<Int?>(null) }
    var itemToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    val nameFocus = remember { FocusRequester() }
    val qtyFocus = remember { FocusRequester() }
    val rateFocus = remember { FocusRequester() }

    var isDropdownExpanded by remember { mutableStateOf(false) }
    var customerSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var cName by remember { mutableStateOf(TextFieldValue(state.customerName)) }

    LaunchedEffect(state.customerName) {
        if (state.customerName != cName.text) { cName = TextFieldValue(state.customerName) }
    }

    // 🚀 FIX #5: Debounce search and perform DB queries on Dispatchers.IO
    LaunchedEffect(cName.text) {
        val query = cName.text.trim()
        if (query.isNotBlank() && isDropdownExpanded) {
            delay(300)
            val rawResults = withContext(Dispatchers.IO) {
                customerDao.searchCustomers("%$query%").map { it.customerName }
            }
            customerSuggestions = rawResults.sortedBy { name ->
                if (name.startsWith(query, ignoreCase = true)) 0 else 1
            }.take(5)
        } else {
            customerSuggestions = emptyList()
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            Toast.makeText(context, "Invoice Saved Successfully", Toast.LENGTH_SHORT).show()
            viewModel.resetSaveState()
        }
    }

    BackHandler(enabled = isDropdownExpanded) {
        isDropdownExpanded = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Invoice", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White, fontFamily = latoFamily) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                    actions = {
                        if (state.invoiceId != 0) {
                            IconButton(onClick = onShareFirebase) { Icon(Icons.Default.Share, contentDescription = "Firebase Share", tint = Color(0xFFFF4444)) }
                        }
                        IconButton(onClick = onResetApp) { Icon(painterResource(id = R.drawable.reset_icon), contentDescription = "Reset", tint = Color.White, modifier = Modifier.size(22.dp)) }
                        IconButton(onClick = { context.startActivity(Intent(context, InvoiceActivity::class.java)) }) { Icon(painterResource(id = R.drawable.invoice_icon), contentDescription = "History", tint = Color.White, modifier = Modifier.size(22.dp)) }
                        IconButton(onClick = {
                            val intent = Intent(context, SettingActivity::class.java)
                            intent.putExtra("invoiceId", state.invoiceId)
                            context.startActivity(intent)
                        }) { Icon(painterResource(id = R.drawable.ic_setting), contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(22.dp)) }
                    }
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSaveInvoice,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) { Text(if (state.invoiceId != 0) "Update" else "Save", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = latoFamily) }

                    Surface(
                        modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = onShareInvoice, // 🚀 FIX #1: Loading handled by ViewModel
                                onLongClick = { showQuickShare = true }
                            ),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp).padding(end = 6.dp))
                            Text("Share", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp, fontFamily = latoFamily)
                        }
                    }
                }
            }
        ) { paddingValues ->

            val density = LocalDensity.current.density

            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

                // --- LAYER 3: CUSTOMER SEARCH HEADER ---
                // 🚀 FIX #6: Anchored suggestion card naturally by wrapping in a Box + Column
                Box(modifier = Modifier.fillMaxWidth().zIndex(100f)) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(3f)) {
                                OutlinedTextField(
                                    value = cName,
                                    onValueChange = {
                                        cName = it
                                        viewModel.setCustomerName(it.text)
                                        isDropdownExpanded = true
                                    },
                                    label = { Text("Customer Name") },
                                    leadingIcon = { Icon(painterResource(id = R.drawable.profile_icon), null, modifier = Modifier.size(20.dp)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = {
                                        isDropdownExpanded = false
                                        nameFocus.requestFocus()
                                    }),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(dateString, modifier = Modifier.weight(1f).padding(top = 16.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = latoFamily)
                        }
                        
                        // 🚀 FIX #11: Replace Column+forEach with LazyColumn for suggestions
                        if (isDropdownExpanded && customerSuggestions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .padding(start = 12.dp)
                                    .shadow(12.dp, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                    items(customerSuggestions, key = { it }) { suggestion ->
                                        Text(
                                            text = suggestion,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .pointerInput(suggestion) {
                                                    awaitEachGesture {
                                                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                                        down.consume()
                                                        cName = TextFieldValue(suggestion, TextRange(suggestion.length))
                                                        viewModel.setCustomerName(suggestion)
                                                        isDropdownExpanded = false
                                                        coroutineScope.launch { delay(150); nameFocus.requestFocus() }
                                                    }
                                                }
                                                .padding(16.dp),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = latoFamily,
                                            color = Color.Black
                                        )
                                        HorizontalDivider(color = Color(0xFFF3F4F6))
                                    }
                                }
                            }
                        }
                    }
                }

                // --- LAYER 1 & 2: BASE UI & OVERLAY ---
                Box(modifier = Modifier.fillMaxSize()) {

                    // -- THE BASE UI --
                    Column(modifier = Modifier.fillMaxSize()) {

                        // 🚀 FIX #9: rememberSaveable to prevent state loss on rotation
                        var itemName by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
                        var itemQty by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(state.defaultQuantity.toString())) }
                        var itemRate by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

                        var isNameFocused by remember { mutableStateOf(false) }
                        var isQtyFocused by remember { mutableStateOf(false) }
                        var isRateFocused by remember { mutableStateOf(false) }

                        val q = itemQty.text.toIntOrNull() ?: 0
                        val r = itemRate.text.toDoubleOrNull() ?: 0.0
                        val itemTotal = q * r

                        fun onAddItem() {
                            val name = itemName.text.trim()
                            if (name.isNotEmpty() && q > 0 && r > 0.0) {
                                viewModel.addItem(TemporaryItem(name, q, r, itemTotal))
                                coroutineScope.launch(Dispatchers.IO) {
                                    val existing = rateDao.getRateByItemName(name)
                                    if (existing != null) { existing.rate = r; rateDao.update(existing) }
                                    else { rateDao.insert(Rate(item_name = name, rate = r)) }
                                }
                                itemName = TextFieldValue(""); itemRate = TextFieldValue("")
                                val defaultQ = state.defaultQuantity
                                itemQty = TextFieldValue(defaultQ.toString(), TextRange(defaultQ.toString().length))
                                nameFocus.requestFocus()
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${state.items.size + 1}.", modifier = Modifier.weight(0.8f), fontSize = 14.sp, fontFamily = latoFamily)

                                BasicTextField(
                                    value = itemName, onValueChange = { itemName = it; itemRate = TextFieldValue("") },
                                    modifier = Modifier.weight(3.7f).padding(end = 8.dp).focusRequester(nameFocus).autoSelectOnFocus(itemName) { itemName = it }.onFocusChanged { isNameFocused = it.isFocused },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                                    visualTransformation = VisualTransformation.None,
                                    keyboardActions = KeyboardActions(onNext = {
                                        qtyFocus.requestFocus()
                                        val nameTrimmed = itemName.text.trim()
                                        if (nameTrimmed.isNotBlank()) {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val entry = rateDao.getRateByItemName(nameTrimmed)
                                                if (entry != null) { withContext(Dispatchers.Main) { itemRate = TextFieldValue(entry.rate.toString(), TextRange(entry.rate.toString().length)) } }
                                            }
                                        }
                                    }),
                                    textStyle = TextStyle(fontSize = 15.sp, color = Color.Black, fontFamily = latoFamily, textAlign = TextAlign.Center), singleLine = true,
                                    cursorBrush = SolidColor(Color.Black),
                                    decorationBox = { inner -> Box(Modifier.background(if(isNameFocused) Color(0xFFE8EAF6) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp)).padding(vertical = 8.dp, horizontal = 4.dp), Alignment.Center) { if (itemName.text.isEmpty()) Text("Design", color = Color.Gray, fontSize = 14.sp) else inner() } }
                                )
                                BasicTextField(
                                    value = itemQty, onValueChange = { itemQty = it },
                                    modifier = Modifier.weight(1.6f).padding(end = 8.dp).focusRequester(qtyFocus).autoSelectOnFocus(itemQty) { itemQty = it }.onFocusChanged { isQtyFocused = it.isFocused },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                                    visualTransformation = VisualTransformation.None,
                                    keyboardActions = KeyboardActions(onNext = { rateFocus.requestFocus() }),
                                    textStyle = TextStyle(fontSize = 15.sp, color = Color.Black, fontFamily = latoFamily, textAlign = TextAlign.Center), singleLine = true,
                                    cursorBrush = SolidColor(Color.Black),
                                    decorationBox = { inner -> Box(Modifier.background(if(isQtyFocused) Color(0xFFE8EAF6) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp)).padding(vertical = 8.dp, horizontal = 4.dp), Alignment.Center) { if (itemQty.text.isEmpty()) Text("Qty", color = Color.Gray, fontSize = 14.sp) else inner() } }
                                )
                                BasicTextField(
                                    value = itemRate, onValueChange = { itemRate = it },
                                    modifier = Modifier.weight(1.9f).padding(end = 8.dp).focusRequester(rateFocus).autoSelectOnFocus(itemRate) { itemRate = it }.onFocusChanged { isRateFocused = it.isFocused },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                                    visualTransformation = VisualTransformation.None,
                                    keyboardActions = KeyboardActions(onDone = { onAddItem() }),
                                    textStyle = TextStyle(fontSize = 15.sp, color = Color.Black, fontFamily = latoFamily, textAlign = TextAlign.Center), singleLine = true,
                                    cursorBrush = SolidColor(Color.Black),
                                    decorationBox = { inner -> Box(Modifier.background(if(isRateFocused) Color(0xFFE8EAF6) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp)).padding(vertical = 8.dp, horizontal = 4.dp), Alignment.Center) { if (itemRate.text.isEmpty()) Text("Rate", color = Color.Gray, fontSize = 14.sp) else inner() } }
                                )
                                Text(fmt(itemTotal), modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                            }
                            if (itemName.text.isNotEmpty() && q > 0 && r > 0.0) {
                                Button(onClick = { onAddItem() }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(44.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Add to List", fontWeight = FontWeight.Bold, fontFamily = latoFamily) }
                            }
                        }

                        // Table Header
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE6E6E6)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("No.", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp, fontFamily = latoFamily)
                            Text("Items", Modifier.weight(3.5f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp, fontFamily = latoFamily)
                            Text("Qty", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                            Text("Rate", Modifier.weight(1.8f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                            Text("Total", Modifier.weight(2.4f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                        }

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            // 🚀 FIX #3: Provide stable keys for LazyColumn items
                            itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                                val bgColor = if (index % 2 == 1) Color(0xFFF9F9F9) else Color.White
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.StartToEnd -> { editItemIndex = index; false }
                                            SwipeToDismissBoxValue.EndToStart -> { itemToDeleteIndex = index; false }
                                            else -> false
                                        }
                                    },
                                    // 🚀 FIX #7: Deliberate 50% threshold for horizontal swipes
                                    positionalThreshold = { totalDistance -> totalDistance * 0.5f }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val direction = dismissState.dismissDirection
                                        val color by animateColorAsState(if (direction == SwipeToDismissBoxValue.StartToEnd) Color(0xFF2196F3) else if (direction == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent, label = "")
                                        Box(modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp), contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) { Icon(if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Edit else Icons.Default.Delete, contentDescription = null, tint = Color.White) }
                                    }
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${index + 1}", Modifier.weight(0.8f), color = Color.DarkGray, fontSize = 14.sp, fontFamily = latoFamily)
                                        Text(item.name, Modifier.weight(3.5f), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = latoFamily)
                                        val defaultQ = state.defaultQuantity
                                        val qtyColor = if (item.quantity == defaultQ) Color.Black else Color(0xFFD32F2F)
                                        val qtyWeight = if (item.quantity == defaultQ) FontWeight.Bold else FontWeight.Black
                                        Text("${item.quantity}", Modifier.weight(1.5f), color = qtyColor, fontWeight = qtyWeight, fontSize = 15.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                                        Text(fmt(item.rate), Modifier.weight(1.8f), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                                        Text(fmt(item.total), Modifier.weight(2.4f), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                                    }
                                }
                            }

                            item {
                                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5E7EB))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(Modifier.weight(4.3f))
                                    Text("${state.totalQuantity} Pcs", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                                    Text("₹ ${fmt(state.subTotal)}", Modifier.weight(4.2f), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                                }
                                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5E7EB))
                            }

                            itemsIndexed(state.discounts, key = { _, d -> d.id }) { index, discount ->
                                val currentIndex by rememberUpdatedState(index)
                                var isDragging by remember { mutableStateOf(false) }
                                var dragOffsetPx by remember { mutableFloatStateOf(0f) }
                                val itemHeightPx = with(LocalDensity.current) { 52.dp.toPx() }

                                val animatedOffset by animateFloatAsState(
                                    targetValue = if (isDragging) dragOffsetPx else 0f,
                                    animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh) else spring(stiffness = Spring.StiffnessMedium),
                                    label = "dragOffset"
                                )

                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.StartToEnd -> { editDiscountIndex = currentIndex; false }
                                            SwipeToDismissBoxValue.EndToStart -> { viewModel.removeDiscountAt(currentIndex); false }
                                            else -> false
                                        }
                                    },
                                    positionalThreshold = { totalDistance -> totalDistance * 0.5f }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val direction = dismissState.dismissDirection
                                        val color by animateColorAsState(
                                            if (direction == SwipeToDismissBoxValue.StartToEnd) Color(0xFF2196F3)
                                            else if (direction == SwipeToDismissBoxValue.EndToStart) Color.Red
                                            else Color.Transparent,
                                            label = ""
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                                            contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Edit else Icons.Default.Delete,
                                                contentDescription = null, tint = Color.White
                                            )
                                        }
                                    }
                                ) {
                                    val elevation by animateDpAsState(
                                        targetValue = if (isDragging) 10.dp else 0.dp,
                                        label = "elevation"
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .zIndex(if (isDragging) 10f else 0f)
                                            .graphicsLayer { translationY = animatedOffset }
                                            .shadow(elevation, RoundedCornerShape(8.dp))
                                            .background(if (isDragging) Color(0xFFF0F4FF) else Color.White)
                                            .pointerInput(index) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        isDragging = true
                                                        dragOffsetPx = 0f
                                                    },
                                                    onDragEnd = {
                                                        isDragging = false
                                                        dragOffsetPx = 0f
                                                    },
                                                    onDragCancel = {
                                                        isDragging = false
                                                        dragOffsetPx = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetPx += dragAmount.y
                                                        val threshold = itemHeightPx * 0.6f
                                                        when {
                                                            dragOffsetPx > threshold && currentIndex < state.discounts.size - 1 -> {
                                                                viewModel.swapDiscounts(currentIndex, currentIndex + 1)
                                                                dragOffsetPx -= itemHeightPx
                                                            }
                                                            dragOffsetPx < -threshold && currentIndex > 0 -> {
                                                                viewModel.swapDiscounts(currentIndex, currentIndex - 1)
                                                                dragOffsetPx += itemHeightPx
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(discount.title, Modifier.weight(3f), color = Color.Black, fontSize = 14.sp, fontFamily = latoFamily)
                                        val pctStr = if (discount.percentage > 0) "${discount.percentage}%" else ""
                                        Text(pctStr, Modifier.weight(1.7f), color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                                        val absAmt = discount.amount.absoluteValue
                                        if (!discount.isPlus) {
                                            Box(modifier = Modifier.weight(2.6f), contentAlignment = Alignment.CenterEnd) {
                                                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(6.dp)) {
                                                    Text("- ₹ ${fmt(absAmt)}", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontFamily = latoFamily)
                                                }
                                            }
                                        } else {
                                            Text("₹ ${fmt(absAmt)}", Modifier.weight(2.6f), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                                        }
                                    }
                                }
                            }

                            item {
                                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Total", Modifier.weight(3f), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp, fontFamily = latoFamily)
                                    Text("₹ ${fmt(state.grandTotal)}", Modifier.weight(4.5f), fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                                }
                                Text(text = "+ Setup Discounts & Colors", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = latoFamily, modifier = Modifier.fillMaxWidth().clickable { showQuickSetup = true }.padding(vertical = 20.dp), textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // -- Suggestion Card ZOMATO style overlay logic simplified --
                    if (isDropdownExpanded && customerSuggestions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .zIndex(50f)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    isDropdownExpanded = false
                                    focusManager.clearFocus()
                                }
                        )
                    }
                }
            }
        }

        // --- DIALOGS DELEGATION ---
        if (itemToDeleteIndex != null) {
            DeleteItemDialog(
                onConfirm = { viewModel.removeItem(itemToDeleteIndex!!); itemToDeleteIndex = null },
                onDismiss = { itemToDeleteIndex = null }
            )
        }
        if (editItemIndex != null) {
            state.items.getOrNull(editItemIndex!!)?.let { item ->
                EditItemDialog(item = item, onSave = { updatedItem -> viewModel.updateItem(editItemIndex!!, updatedItem); editItemIndex = null }, onDismiss = { editItemIndex = null })
            }
        }
        if (editDiscountIndex != null) {
            state.discounts.getOrNull(editDiscountIndex!!)?.let { discount ->
                EditDiscountDialog(discount = discount, onSave = { updatedDiscount -> viewModel.updateDiscount(editDiscountIndex!!, updatedDiscount); editDiscountIndex = null }, onDismiss = { editDiscountIndex = null })
            }
        }
        if (showQuickSetup) QuickSetupBottomSheet(onDismiss = { showQuickSetup = false }, viewModel = viewModel, database = database)
        if (showQuickShare) QuickShareBottomSheet(onDismiss = { showQuickShare = false }, viewModel = viewModel, onShare = onShareInvoice)
        
        // 🚀 FIX #1: Unified loading state reactive to state.isLoading
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        }
    }
}
