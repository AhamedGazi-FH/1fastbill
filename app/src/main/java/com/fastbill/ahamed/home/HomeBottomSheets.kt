package com.fastbill.ahamed.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.fastbill.ahamed.InvoiceViewModel
import com.fastbill.ahamed.database.InvoiceDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickSetupBottomSheet(
    onDismiss: () -> Unit,
    viewModel: InvoiceViewModel,
    database: InvoiceDatabase
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val uiState by viewModel.uiState.collectAsState()
        var discounts by remember { mutableStateOf<List<com.fastbill.ahamed.database.Discount>>(emptyList()) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) { discounts = database.discountDao().getAllDiscountsSorted() }
        }

        // 🚀 FIX #2: Use availableColors from uiState (loaded off-thread in ViewModel)
        val colorList = uiState.availableColors

        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 16.dp)) {

            Box(
                modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color(0xFFE5E7EB)).align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(20.dp))

            val negativeDiscounts = discounts.filter { !it.isPlus }
            val positiveDiscounts = discounts.filter { it.isPlus }

            if (negativeDiscounts.isNotEmpty()) {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    negativeDiscounts.forEach { discount ->
                        val isSelected = uiState.discounts.any { it.id == discount.id }
                        val valText = if (discount.percentage > 0) "${discount.percentage}%" else "₹${discount.price.toInt()}"
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (!isSelected) viewModel.addDiscount(discount) else viewModel.removeDiscount(discount.id) },
                            label = { Text("${discount.title} ($valText)", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFEE2E2), selectedLabelColor = Color(0xFFB91C1C), containerColor = Color(0xFFF3F4F6)),
                            shape = RoundedCornerShape(10.dp), border = null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (positiveDiscounts.isNotEmpty()) {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    positiveDiscounts.forEach { discount ->
                        val isSelected = uiState.discounts.any { it.id == discount.id }
                        val valText = if (discount.percentage > 0) "${discount.percentage}%" else "₹${discount.price.toInt()}"
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (!isSelected) viewModel.addDiscount(discount) else viewModel.removeDiscount(discount.id) },
                            label = { Text("${discount.title} ($valText)", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD1FAE5), selectedLabelColor = Color(0xFF047857), containerColor = Color(0xFFF3F4F6)),
                            shape = RoundedCornerShape(10.dp), border = null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                colorList.forEach { colorStr ->
                    val parsedColor = try { colorStr.toColorInt() } catch(_: Exception) { android.graphics.Color.GRAY }
                    val isSelected = colorStr == uiState.themeColor
                    val targetSize by animateDpAsState(if (isSelected) 44.dp else 36.dp, label = "")

                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).clickable {
                            viewModel.saveSelectedColorPreference(colorStr)
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(targetSize).clip(CircleShape).background(Color(parsedColor)))
                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827))) {
                Text("DONE", fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickShareBottomSheet(
    onDismiss: () -> Unit,
    viewModel: InvoiceViewModel,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val uiState by viewModel.uiState.collectAsState()
        val numbers = listOfNotNull(uiState.shareNumber1.takeIf { it.isNotEmpty() }, uiState.shareNumber2.takeIf { it.isNotEmpty() }, uiState.shareNumber3.takeIf { it.isNotEmpty() })

        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp)) {

            Box(
                modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color(0xFFE5E7EB)).align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    listOf("wa" to "WhatsApp", "wa_biz" to "WA Biz", "other" to "Other").forEach { (code, label) ->
                        val isSel = uiState.shareApp == code
                        Surface(color = if (isSel) Color.White else Color.Transparent, shape = RoundedCornerShape(8.dp), shadowElevation = if (isSel) 2.dp else 0.dp, modifier = Modifier.weight(1f).clickable { viewModel.saveShareAppPreference(code) }) {
                            Text(label, color = if (isSel) Color(0xFF111827) else Color(0xFF6B7280), fontSize = 13.sp, fontWeight = if(isSel) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFE5E7EB)), color = Color.White) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("Auto-Caption", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), modifier = Modifier.weight(1f))
                        Switch(checked = uiState.isCaptionOn, onCheckedChange = { viewModel.saveCaptionPreference(it) }, modifier = Modifier.scale(0.85f))
                    }
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("Direct Send", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), modifier = Modifier.weight(1f))
                        Switch(checked = uiState.isNumberOn, onCheckedChange = { viewModel.saveNumberOnPreference(it) }, modifier = Modifier.scale(0.85f))
                    }
                }
            }

            AnimatedVisibility(visible = uiState.isNumberOn && numbers.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    numbers.forEach { num ->
                        val isSelected = uiState.defaultShareNumber == num
                        Surface(color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color(0xFFF9FAFB), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE5E7EB)), modifier = Modifier.clickable { viewModel.saveDefaultShareNumberPreference(num) }) {
                            Text(num, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF374151))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (uiState.customerName.isNotEmpty() && uiState.items.isNotEmpty()) {
                        onDismiss()
                        onShare()
                    } else Toast.makeText(context, "Add items and name first", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 8.dp))
                Text("QUICK SHARE NOW", fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
