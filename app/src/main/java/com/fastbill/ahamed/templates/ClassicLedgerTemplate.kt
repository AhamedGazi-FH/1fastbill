package com.fastbill.ahamed.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// ==========================================================
// 🧾 TEMPLATE 2: CLASSIC (Old School Ledger Style)
// ==========================================================
@Composable
fun TemplateClassic(
    customerName: String, date: String, items: List<TemporaryItem>, discounts: List<Discount>,
    totalQuantity: Int, subTotal: Double, grandTotal: Double, themeColorHex: String
) {
    val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    fun formatNumber(amount: Double): String = indianFormat.format(amount.roundToInt())

    val themeColor = try { Color(android.graphics.Color.parseColor(themeColorHex)) } catch(e: Exception) { Color(0xFF3F51B5) }
    val isDarkTheme = themeColor.luminance() < 0.5f
    val footerTextColor = if (isDarkTheme) Color.White else Color.Black

    val latoFamily = FontFamily(Font(R.font.lato_regular, FontWeight.Normal), Font(R.font.lato_bold, FontWeight.Bold))

    val textBlack = Color(0xFF000000)
    val textMuted = Color(0xFF666666)
    val bgHeader = Color(0xFFEBEBEB)
    val bgStripe = Color(0xFFF5F5F5)
    val itemDivider = Color(0xFFCCCCCC)

    Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(themeColor))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("BILLED TO", fontSize = 11.sp, color = textMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                Text(customerName.ifEmpty { "Cash Customer" }, fontSize = 28.sp, fontWeight = FontWeight.Black, color = textBlack, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = latoFamily, softWrap = false)
            }
            Text(date, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textBlack, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
        }

        HorizontalDivider(thickness = 1.dp, color = itemDivider)
        Row(modifier = Modifier.fillMaxWidth().background(bgHeader).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("No.", Modifier.width(35.dp), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 13.sp, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
            Text("Item", Modifier.weight(1f).padding(end = 4.dp), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 13.sp, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            Text("Qty", Modifier.width(50.dp).padding(end = 5.dp), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 13.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
            Text("Rate", Modifier.width(70.dp).padding(end = 5.dp), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 13.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
            Text("Total", Modifier.width(88.dp), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 13.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
        }
        HorizontalDivider(thickness = 1.dp, color = itemDivider)

        items.forEachIndexed { index, item ->
            val bgColor = if (index % 2 == 1) bgStripe else Color.White
            Row(modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", Modifier.width(35.dp), color = textMuted, fontWeight = FontWeight.Medium, fontSize = 14.sp, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                Text(item.name, Modifier.weight(1f).padding(end = 4.dp), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                Text("${item.quantity}", Modifier.width(50.dp).padding(end = 5.dp), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                Text(formatNumber(item.rate), Modifier.width(70.dp).padding(end = 5.dp), color = textMuted, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                Text(formatNumber(item.total), Modifier.width(88.dp), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
            }
            HorizontalDivider(thickness = 1.dp, color = itemDivider)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Subtotal", Modifier.width(35.dp), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 12.sp, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
            Spacer(Modifier.weight(1f))
            Text("${totalQuantity} Pcs", Modifier.width(50.dp).padding(end = 5.dp), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 15.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
            Spacer(Modifier.width(70.dp))
            Text("₹ ${formatNumber(subTotal)}", Modifier.width(88.dp), fontWeight = FontWeight.Black, color = textBlack, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
        }
        HorizontalDivider(thickness = 1.dp, color = itemDivider)

        discounts.forEach { discount ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(35.dp))
                Text(discount.title, Modifier.weight(1f).padding(end = 4.dp), color = textBlack, fontWeight = FontWeight.Medium, fontSize = 14.sp, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                val pctStr = if (discount.percentage > 0) "${discount.percentage}%" else ""
                Text(pctStr, Modifier.width(50.dp + 70.dp), color = textBlack, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                
                val absoluteAmount = discount.amount.absoluteValue
                val prefix = if (discount.isPlus) "+ ₹" else "- ₹"
                val tColor = if (discount.isPlus) textBlack else Color(0xFFD32F2F)
                Text("$prefix ${formatNumber(absoluteAmount)}", Modifier.width(88.dp), color = tColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
            }
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0F0F0))
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = themeColor, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NET PAYABLE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = footerTextColor.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("TOTAL: ${totalQuantity} PCS", fontWeight = FontWeight.Bold, color = footerTextColor, fontSize = 14.sp, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                    }
                    Text("₹ ${formatNumber(grandTotal)}", fontWeight = FontWeight.Black, color = footerTextColor, fontSize = 28.sp, textAlign = TextAlign.End, fontFamily = latoFamily, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                }
            }
        }
    }
}
