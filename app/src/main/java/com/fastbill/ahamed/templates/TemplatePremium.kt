package com.fastbill.ahamed.templates

import android.content.Context
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
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
// 🧾 TEMPLATE 3: PREMIUM (The Premium "Net Payable" Style)
// ==========================================================
@Composable
fun TemplatePremium(
    customerName: String, date: String, items: List<TemporaryItem>, discounts: List<Discount>,
    totalQuantity: Int, subTotal: Double, grandTotal: Double, themeColorHex: String
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val defaultQty = remember(isPreview) {
        if (isPreview) 4 else {
            try { context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).getInt("default_quantity", 4) }
            catch (_: Exception) { 4 }
        }
    }

    val format = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    fun fmt(amt: Double): String = format.format(amt.roundToInt())

    val themeColor = try { Color(android.graphics.Color.parseColor(themeColorHex)) } catch(_: Exception) { Color(0xFF000080) }
    val footerTextColor = if (themeColor.luminance() < 0.5f) Color.White else Color.Black

    val textBlack = Color(0xFF111827)
    val textMuted = Color(0xFF6B7280)
    val textRed = Color(0xFFD32F2F)
    val bgStripe = Color(0xFFF9FAFB)
    val bgPill = Color(0xFFF3F4F6)

    val latoFamily = FontFamily(
        Font(R.font.lato_regular, FontWeight.Normal),
        Font(R.font.lato_bold, FontWeight.Bold)
    )

    Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 20.dp)) {

        // --- 1. TOP THEME BAR (BILL SUMMARY) ---
        Surface(color = themeColor, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "BILL SUMMARY",
                color = footerTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp),
                fontFamily = latoFamily
            )
        }

        // --- 2. HEADER (BILLED TO & DATE PILL) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text("BILLED TO", fontSize = 11.sp, color = textMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = latoFamily)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = customerName.ifEmpty { "Cash Customer" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = textBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = latoFamily
                )
            }
            Surface(color = bgPill, shape = RoundedCornerShape(8.dp)) {
                Text(date, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textBlack, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontFamily = latoFamily)
            }
        }

        // --- 3. TABLE HEADERS ---
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("NO.", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 13.sp, letterSpacing = 0.5.sp, fontFamily = latoFamily)
            Text("ITEMS", Modifier.weight(2.5f), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 13.sp, letterSpacing = 0.5.sp, fontFamily = latoFamily)
            Text("QTY", Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 13.sp, textAlign = TextAlign.Center, letterSpacing = 0.5.sp, fontFamily = latoFamily)
            Text("RATE", Modifier.weight(1.8f), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 13.sp, textAlign = TextAlign.End, letterSpacing = 0.5.sp, fontFamily = latoFamily)
            Text("TOTAL", Modifier.weight(2.2f), fontWeight = FontWeight.Bold, color = textMuted, fontSize = 13.sp, textAlign = TextAlign.End, letterSpacing = 0.5.sp, fontFamily = latoFamily)
        }

        // --- 4. DATA ROWS ---
        items.forEachIndexed { index, item ->
            val bgColor = if (index % 2 == 1) bgStripe else Color.White
            Row(modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", Modifier.weight(0.8f), color = textMuted, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = latoFamily)
                Text(item.name, Modifier.weight(2.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = latoFamily)

                val qtyColor = if (item.quantity == defaultQty) textBlack else textRed
                Text("${item.quantity}", Modifier.weight(1.2f), color = qtyColor, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center, fontFamily = latoFamily)

                Text(fmt(item.rate), Modifier.weight(1.8f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                Text(fmt(item.total), Modifier.weight(2.2f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 5. DASHED DIVIDER ---
        Canvas(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp)) {
            drawLine(
                color = Color(0xFFE5E7EB),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f),
                strokeWidth = 2f
            )
        }

        // --- 6. SUBTOTAL & DISCOUNTS ---
        Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(3.3f))

            // EXPERT FIX: Big Pill, Big Font, Zero Clipping
            Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                Text(
                    text = "$totalQuantity Pcs",
                    fontWeight = FontWeight.Black,
                    color = textBlack,
                    fontSize = 16.sp, // Font wapas bada kar diya
                    maxLines = 1,
                    softWrap = false, // Do lines me nahi tootega
                    modifier = Modifier
                        .wrapContentWidth(unbounded = true) // Column ki width se aazad (koi cutting nahi)
                        .background(bgPill, RoundedCornerShape(12.dp)) // Premium roundness
                        .padding(horizontal = 16.dp, vertical = 8.dp), // Pill ko bada kar diya (Extra padding)
                    fontFamily = latoFamily,
                    textAlign = TextAlign.Center
                )
            }

            Text("₹ ${fmt(subTotal)}", Modifier.weight(4.0f), fontWeight = FontWeight.Black, color = textBlack, fontSize = 18.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
        }

        discounts.forEachIndexed { index, discount ->
            val isNegative = !discount.isPlus
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(0.8f))

                Text(
                    text = discount.title,
                    modifier = Modifier.weight(2.5f).padding(end = 4.dp),
                    color = textBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = latoFamily
                )

                val pctStr = if (discount.percentage > 0) "${discount.percentage}%" else ""
                Text(pctStr, Modifier.weight(1.2f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, fontFamily = latoFamily)

                val amt = try { discount.amount.absoluteValue } catch(_: Exception) { 0.0 }
                val prefix = if (isNegative) "- ₹ " else "+ ₹ "
                val tColor = if (isNegative) textRed else textBlack
                Text("$prefix${fmt(amt)}", Modifier.weight(4.0f), color = tColor, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
            }

            if (index < discounts.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(start = 20.dp, end = 20.dp), thickness = 1.dp, color = Color(0xFFF3F4F6))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 7. THE MASTERPIECE PILL (NET PAYABLE) ---
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = themeColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NET PAYABLE", fontWeight = FontWeight.Bold, color = footerTextColor, fontSize = 15.sp, letterSpacing = 1.sp, fontFamily = latoFamily)
                    Spacer(Modifier.weight(1f))
                    Text("₹ ${fmt(grandTotal)}", fontWeight = FontWeight.Black, color = footerTextColor, fontSize = 32.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                }
            }
        }
    }
}