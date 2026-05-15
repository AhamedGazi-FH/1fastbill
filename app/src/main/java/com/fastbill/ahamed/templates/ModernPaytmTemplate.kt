package com.fastbill.ahamed.templates

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastbill.ahamed.R
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.model.TemporaryItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// ==========================================================
// 🎨 TEMPLATE 1: MODERN (The Perfected Paytm/Cred Style)
// ==========================================================
@Composable
fun TemplateModern(
    customerName: String, date: String, items: List<TemporaryItem>, discounts: List<Discount>,
    totalQuantity: Int, subTotal: Double, grandTotal: Double, themeColorHex: String
) {
    val context = LocalContext.current
    val defaultQty = remember { context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).getInt("default_quantity", 4) }

    val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    fun formatNumber(amount: Double): String = indianFormat.format(amount.roundToInt())

    val formattedDate = remember(date) {
        try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            inputFormat.parse(date)?.let { outputFormat.format(it) } ?: date
        } catch (e: Exception) { date }
    }

    val themeColor = try { Color(android.graphics.Color.parseColor(themeColorHex)) } catch(e: Exception) { Color(0xFF3F51B5) }
    val isDarkTheme = themeColor.luminance() < 0.5f
    val footerTextColor = if (isDarkTheme) Color.White else Color.Black

    val latoFamily = FontFamily(Font(R.font.lato_regular, FontWeight.Normal), Font(R.font.lato_bold, FontWeight.Bold))

    val textBlack = Color(0xFF111111)
    val textMuted = Color(0xFF555555)
    val bgHeader = Color(0xFFF3F4F6)
    val bgStripe = Color(0xFFF9F9F9)
    val itemDivider = Color(0xFFEAEAEA)
    val chipGreenBg = Color(0xFFE8F5E9)
    val chipGreenText = Color(0xFF1B5E20)

    Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(themeColor))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(customerName.ifEmpty { "Cash Customer" }, fontSize = 32.sp, fontWeight = FontWeight.Black, color = textBlack, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 12.dp), fontFamily = latoFamily)
            Surface(color = bgHeader, shape = RoundedCornerShape(8.dp)) {
                Text(formattedDate, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textBlack, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontFamily = latoFamily)
            }
        }

        HorizontalDivider(thickness = 1.dp, color = itemDivider)
        Row(modifier = Modifier.fillMaxWidth().background(bgHeader).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("No.", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 14.sp, fontFamily = latoFamily)
            Text("Item", Modifier.weight(3.5f), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 14.sp, fontFamily = latoFamily)
            Text("Qty", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
            Text("Rate", Modifier.weight(2f), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
            Text("Total", Modifier.weight(2.5f), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
        }
        HorizontalDivider(thickness = 1.dp, color = itemDivider)

        items.forEachIndexed { index, item ->
            val bgColor = if (index % 2 == 1) bgStripe else Color.White
            Row(modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", Modifier.weight(1f), color = textMuted, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = latoFamily)
                Text(item.name, Modifier.weight(3.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = latoFamily)

                // Smart Anomaly Highlight (Deep Red Text, Perfect Alignment)
                if (item.quantity == defaultQty) {
                    Text("${item.quantity}", Modifier.weight(1.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                } else {
                    Text("${item.quantity}", Modifier.weight(1.5f), color = Color(0xFFD32F2F), fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                }

                Text(formatNumber(item.rate), Modifier.weight(2f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                Text(formatNumber(item.total), Modifier.weight(2.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
            }
            HorizontalDivider(thickness = 1.dp, color = itemDivider)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Text("SUBTOTAL", Modifier.weight(3.5f), fontWeight = FontWeight.Black, color = textMuted, fontSize = 14.sp, fontFamily = latoFamily)
            Text("${totalQuantity} Pcs", Modifier.weight(1.5f), fontWeight = FontWeight.Black, color = textBlack, fontSize = 18.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
            Spacer(Modifier.weight(2f))
            Text("₹ ${formatNumber(subTotal)}", Modifier.weight(2.5f), fontWeight = FontWeight.Black, color = textBlack, fontSize = 18.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
        }
        HorizontalDivider(thickness = 1.dp, color = itemDivider)

        discounts.forEach { discount ->
            val isNegative = !discount.isPlus
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text(discount.title, Modifier.weight(3.5f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = latoFamily)
                val pctStr = if (discount.percentage > 0) "${discount.percentage}%" else ""
                Text(pctStr, Modifier.weight(1.5f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                Spacer(Modifier.weight(2f))
                val absoluteAmount = discount.amount.absoluteValue

                if (isNegative) {
                    Box(modifier = Modifier.weight(2.5f), contentAlignment = Alignment.CenterEnd) {
                        Surface(color = chipGreenBg, shape = RoundedCornerShape(6.dp)) {
                            Text("- ₹ ${formatNumber(absoluteAmount)}", color = chipGreenText, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontFamily = latoFamily)
                        }
                    }
                } else {
                    Text("₹ ${formatNumber(absoluteAmount)}", Modifier.weight(2.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = themeColor, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("TOTAL: ${totalQuantity} PCS", fontWeight = FontWeight.Black, color = footerTextColor, fontSize = 18.sp, fontFamily = latoFamily)
                    Spacer(Modifier.weight(1f))
                    Text("₹ ${formatNumber(grandTotal)}", fontWeight = FontWeight.Black, color = footerTextColor, fontSize = 34.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                }
            }
        }
    }
}