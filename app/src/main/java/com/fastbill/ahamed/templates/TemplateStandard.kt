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
// 📜 TEMPLATE 4: STANDARD (The Exact Ledger Clone)
// ==========================================================
@Composable
fun TemplateStandard(
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

    // Commas format for Subtotal, GrandTotal, Rate and Item Total
    val formatCommas = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    fun fmtCommas(amt: Double): String = formatCommas.format(amt.roundToInt())

    val themeColor = try { Color(android.graphics.Color.parseColor(themeColorHex)) } catch(_: Exception) { Color(0xFFFFC107) }
    val footerTextColor = if (themeColor.luminance() > 0.9f) Color.Black else Color.White

    val textBlack = Color(0xFF111111)
    val textRed = Color(0xFFD32F2F)
    val bgHeader = Color(0xFFE0E0E0)
    val bgStripe = Color(0xFFF5F5F5)

    // AUDIT FIX: Using a single Row background instead of redundant Chips
    val bgRedRow = Color(0xFFFCE4E4)
    val textRedChip = Color(0xFFB91C1C)

    // Forced Lato Custom Font instead of Default Mobile Font
    val latoFamily = FontFamily(
        Font(R.font.lato_regular, FontWeight.Normal),
        Font(R.font.lato_bold, FontWeight.Bold)
    )

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {

        // --- 1. HEADER (Compact Padding Kept Same) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = customerName.ifEmpty { "Cash Customer" },
                fontSize = 28.sp, // EXPERT FIX: Increased font size for Party Name
                fontWeight = FontWeight.Black, // EXPERT FIX: Made Black for maximum boldness
                color = textBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                fontFamily = latoFamily
            )
            Text(
                text = date,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textBlack,
                fontFamily = latoFamily
            )
        }

        // --- 2. TABLE HEADERS ---
        Row(modifier = Modifier.fillMaxWidth().background(bgHeader).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("No.", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 16.sp, fontFamily = latoFamily)
            Text("Items", Modifier.weight(2.5f), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 16.sp, fontFamily = latoFamily)
            Text("Qty", Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 16.sp, textAlign = TextAlign.Center, fontFamily = latoFamily)
            Text("Rate", Modifier.weight(1.8f), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
            Text("Total", Modifier.weight(2.5f), fontWeight = FontWeight.Bold, color = textBlack, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
        }

        // --- 3. DATA ROWS (Padding kept 8.dp to avoid making bill long, but font increased) ---
        items.forEachIndexed { index, item ->
            val bgColor = if (index % 2 == 1) bgStripe else Color.White
            Row(
                modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}", Modifier.weight(0.8f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = latoFamily)

                // EXPERT FIX: Item names and values are larger (17.sp) and bolder
                Text(item.name, Modifier.weight(2.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 17.sp, fontFamily = latoFamily)

                val qtyColor = if (item.quantity == defaultQty) textBlack else textRed
                Text("${item.quantity}", Modifier.weight(1.2f), color = qtyColor, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.Center, fontFamily = latoFamily)

                Text(fmtCommas(item.rate), Modifier.weight(1.8f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily)

                Text(fmtCommas(item.total), Modifier.weight(2.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
            }
        }

        // --- 4. DIVIDER ---
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = Color(0xFFE0E0E0), thickness = 1.dp)

        // --- 5. SUBTOTAL ---
        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(3.3f))

            // EXPERT FIX: Pcs text made bolder (Black) and slightly larger
            Text(
                text = "$totalQuantity Pcs",
                modifier = Modifier.weight(1.2f).wrapContentWidth(unbounded = true),
                fontWeight = FontWeight.Black,
                color = textBlack,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                fontFamily = latoFamily
            )

            Text("₹ ${fmtCommas(subTotal)}", Modifier.weight(4.3f), fontWeight = FontWeight.Black, color = textBlack, fontSize = 19.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
        }

        // --- 6. DISCOUNTS ---
        discounts.forEach { discount ->
            val isNegative = !discount.isPlus
            val rowBg = if (isNegative) bgRedRow else Color.Transparent

            Row(modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(0.8f))

                // EXPERT FIX: Discount list font size increased to 16.sp
                Text(discount.title, Modifier.weight(2.5f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = latoFamily)

                val pctStr = if (discount.percentage > 0) "${discount.percentage}%" else ""
                Text(pctStr, Modifier.weight(1.2f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, fontFamily = latoFamily)

                val amt = try { discount.amount.absoluteValue } catch(_: Exception) { 0.0 }

                if (isNegative) {
                    // AUDIT FIX: Removed the invisible Box and Surface. Zero GPU overdraw.
                    Text(
                        text = "- ₹ ${fmtCommas(amt)}",
                        modifier = Modifier.weight(4.3f),
                        color = textRedChip,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        textAlign = TextAlign.End,
                        fontFamily = latoFamily
                    )
                } else {
                    Text("+ ₹ ${fmtCommas(amt)}", Modifier.weight(4.3f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 7. FOOTER ---
        Row(modifier = Modifier.fillMaxWidth().background(themeColor).padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Total", Modifier.weight(1f), fontWeight = FontWeight.Black, color = footerTextColor, fontSize = 22.sp, fontFamily = latoFamily)

            // EXPERT FIX: Final Total made bigger (26.sp) and Bolder
            Text("₹ ${fmtCommas(grandTotal)}", Modifier.weight(1f), fontWeight = FontWeight.Black, color = footerTextColor, fontSize = 26.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
        }
    }
}