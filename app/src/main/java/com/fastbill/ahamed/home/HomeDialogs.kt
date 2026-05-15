package com.fastbill.ahamed.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.model.TemporaryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- EXPERT UTILITY: Surgical Fix for Memory Leak & Multiple Listeners ---
fun Modifier.autoSelectOnFocus(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    var previouslyFocused by remember { mutableStateOf(false) }

    this.onFocusChanged { focusState ->
        if (focusState.isFocused && !previouslyFocused) {
            scope.launch {
                delay(100)
                onValueChange(value.copy(selection = TextRange(0, value.text.length)))
            }
        }
        previouslyFocused = focusState.isFocused
    }
}

@Composable
fun DeleteItemDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Item", fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to remove this item from the bill?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditItemDialog(
    item: TemporaryItem,
    onSave: (TemporaryItem) -> Unit,
    onDismiss: () -> Unit
) {
    var eName by remember { mutableStateOf(TextFieldValue(item.name)) }
    var eQty by remember { mutableStateOf(TextFieldValue(item.quantity.toString())) }
    var eRate by remember { mutableStateOf(TextFieldValue(item.rate.toString())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = eName, onValueChange = { eName = it }, label = { Text("Item Name") },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.autoSelectOnFocus(eName) { eName = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = eQty, onValueChange = { eQty = it }, label = { Text("Qty") },
                        modifier = Modifier.weight(1f).autoSelectOnFocus(eQty) { eQty = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = eRate, onValueChange = { eRate = it }, label = { Text("Rate") },
                        modifier = Modifier.weight(1f).autoSelectOnFocus(eRate) { eRate = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val nq = eQty.text.toIntOrNull() ?: 0
                val nr = eRate.text.toDoubleOrNull() ?: 0.0
                onSave(TemporaryItem(eName.text, nq, nr, nq * nr))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditDiscountDialog(
    discount: Discount,
    onSave: (Discount) -> Unit,
    onDismiss: () -> Unit
) {
    var dTitle by remember { mutableStateOf(TextFieldValue(discount.title)) }
    var dPct by remember { mutableStateOf(TextFieldValue(if (discount.percentage > 0) discount.percentage.toString() else "")) }
    var dPrice by remember { mutableStateOf(TextFieldValue(if (discount.price > 0) discount.price.toString() else "")) }
    var dIsPlus by remember { mutableStateOf(discount.isPlus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Template", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dTitle, onValueChange = { dTitle = it }, label = { Text("Title") },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.autoSelectOnFocus(dTitle) { dTitle = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dPct, onValueChange = { dPct = it }, label = { Text("%") },
                        modifier = Modifier.weight(1f).autoSelectOnFocus(dPct) { dPct = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = dPrice, onValueChange = { dPrice = it }, label = { Text("Fixed ₹") },
                        modifier = Modifier.weight(1f).autoSelectOnFocus(dPrice) { dPrice = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !dIsPlus, onClick = { dIsPlus = false }, label = { Text("Discount (-)") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = dIsPlus, onClick = { dIsPlus = true }, label = { Text("Charge (+)") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updated = discount.copy(
                    title = dTitle.text,
                    percentage = dPct.text.toIntOrNull() ?: 0,
                    price = dPrice.text.toDoubleOrNull() ?: 0.0,
                    isPlus = dIsPlus
                )
                onSave(updated)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
