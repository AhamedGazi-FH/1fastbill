package com.fastbill.ahamed

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.model.TemporaryItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPrintView(
    items: List<TemporaryItem>,
    discounts: List<Discount>,
    totalQuantity: Int,
    subTotal: Double,
    grandTotal: Double,
    themeColorHex: String,
    onEditItem: (Int, TemporaryItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onEditDiscount: (Int) -> Unit,
    onDeleteDiscount: (Int) -> Unit
) {
    fun formatAmt(amount: Double): String = String.format(Locale.US, "%.2f", amount)

    val themeColor = try { Color(android.graphics.Color.parseColor(themeColorHex)) } catch(e: Exception) { Color(0xFF000080) }

    var editItemIndex by remember { mutableStateOf<Int?>(null) }

    if (editItemIndex != null) {
        val item = items.getOrNull(editItemIndex!!)
        if (item != null) {
            var editName by remember { mutableStateOf(item.name) }
            var editQty by remember { mutableStateOf(item.quantity.toString()) }
            var editRate by remember { mutableStateOf(item.rate.toString()) }

            AlertDialog(
                onDismissRequest = { editItemIndex = null },
                title = { Text("Edit Item", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Item Name") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = editQty, onValueChange = { editQty = it }, label = { Text("Quantity") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = editRate, onValueChange = { editRate = it }, label = { Text("Rate") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), shape = RoundedCornerShape(12.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newQty = editQty.toIntOrNull() ?: 0
                        val newRate = editRate.toDoubleOrNull() ?: 0.0
                        onEditItem(editItemIndex!!, TemporaryItem(editName, newQty, newRate, newQty * newRate))
                        editItemIndex = null
                    }) { Text("Save", fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { editItemIndex = null }) { Text("Cancel") } }
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFE6E6E6)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("No.", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
            Text("Items", Modifier.weight(3f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
            Text("Qty", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End)
            Text("Rate", Modifier.weight(1.9f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End)
            Text("Total", Modifier.weight(2.6f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End)
        }

        items.forEachIndexed { index, item ->
            val bgColor = if (index % 2 == 1) Color(0xFFF0F0F0) else Color.White
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    when (value) {
                        SwipeToDismissBoxValue.StartToEnd -> { editItemIndex = index; false }
                        SwipeToDismissBoxValue.EndToStart -> { onDeleteItem(index); false }
                        else -> false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val direction = dismissState.dismissDirection
                    val color by animateColorAsState(
                        targetValue = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2196F3)
                            SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935)
                            else -> Color.Transparent
                        }, label = "swipeColor"
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        else if (direction == SwipeToDismissBoxValue.EndToStart) Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            ) {
                Row(modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", Modifier.weight(1f), color = Color.Black, fontSize = 14.sp)
                    Text(item.name, Modifier.weight(3f), color = Color.Black, fontSize = 14.sp)
                    Text("${item.quantity}", Modifier.weight(1.5f), color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End)
                    Text(formatAmt(item.rate), Modifier.weight(1.9f), color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End)
                    Text(formatAmt(item.total), Modifier.weight(2.6f), color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End)
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE5E7EB))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(4f)) 
            Text("${totalQuantity} Pcs", Modifier.weight(1.8f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp, textAlign = TextAlign.End)
            Spacer(Modifier.weight(1.8f)) 
            Text(formatAmt(subTotal), Modifier.weight(2.4f), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 15.sp, textAlign = TextAlign.End)
        }

        discounts.forEachIndexed { index, discount ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    when (value) {
                        SwipeToDismissBoxValue.StartToEnd -> { onEditDiscount(index); false }
                        SwipeToDismissBoxValue.EndToStart -> { onDeleteDiscount(index); false }
                        else -> false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val direction = dismissState.dismissDirection
                    val color by animateColorAsState(
                        targetValue = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2196F3)
                            SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935)
                            else -> Color.Transparent
                        }, label = "swipeColor"
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        else if (direction == SwipeToDismissBoxValue.EndToStart) Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            ) {
                Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(0.5f))
                    Text(discount.title, Modifier.weight(3f), color = Color.Black, fontSize = 14.sp)
                    Spacer(Modifier.weight(1.7f))
                    val pctStr = if (discount.percentage > 0) "${discount.percentage}%" else ""
                    Text(pctStr, Modifier.weight(2.2f), color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End)
                    val absoluteAmount = if (discount.percentage > 0) subTotal * (discount.percentage / 100.0) else discount.price
                    Text(formatAmt(absoluteAmount), Modifier.weight(2.6f), color = Color.Black, fontSize = 14.sp, textAlign = TextAlign.End)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(themeColor).padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(0.5f))
            Text("Total", Modifier.weight(3f), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.weight(2f)) 
            Text(formatAmt(grandTotal), Modifier.weight(4.5f), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.End)
        }
    }
}