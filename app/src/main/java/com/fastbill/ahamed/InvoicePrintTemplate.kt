package com.fastbill.ahamed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.model.TemporaryItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun InvoicePrintTemplate(
    customerName: String,
    date: String,
    items: List<TemporaryItem>,
    discounts: List<Discount>,
    totalQuantity: Int,
    subTotal: Double,
    grandTotal: Double,
    themeColorHex: String
) {
    val indianFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    fun formatNumber(amount: Double): String = indianFormat.format(amount)

    // EXPERT: Smart Date Formatter (12-05-2026 -> 12 May 2026)
    val formattedDate = remember(date) {
        try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            inputFormat.parse(date)?.let { outputFormat.format(it) } ?: date
        } catch (e: Exception) {
            date
        }
    }

    val themeColor = try { Color(android.graphics.Color.parseColor(themeColorHex)) } catch(e: Exception) { Color(0xFF3F51B5) }

    val isDarkTheme = themeColor.luminance() < 0.5f
    val footerTextColor = if (isDarkTheme) Color.White else Color.Black

    val latoFamily = FontFamily(
        Font(R.font.lato_regular, FontWeight.Normal),
        Font(R.font.lato_bold, FontWeight.Bold)
    )

    // Strictly High-Contrast Palette
    val textBlack = Color(0xFF000000)
    val textDarkGray = Color(0xFF424242)
    val bgHeader = Color(0xFFF0F0F0)
    val bgStripe = Color(0xFFFAFAFA)
    val dividerColor = Color(0xFFE0E0E0)

    // Expert Chip Colors
    val chipGreenBg = Color(0xFFE8F5E9)
    val chipGreenText = Color(0xFF1B5E20)

    val currentDensity = LocalDensity.current

    // UNIVERSAL SCALING LOCK
    CompositionLocalProvider(
        LocalDensity provides Density(density = currentDensity.density, fontScale = 1f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 8.dp)) {

            // --- 1. ACCENT TOP BAR ---
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(themeColor))

            // --- 2. JUMBO HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customerName.ifEmpty { "Cash Customer" },
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = textBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    fontFamily = latoFamily
                )

                // Expert Date Chip
                Surface(
                    color = bgHeader,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDarkGray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontFamily = latoFamily
                    )
                }
            }

            // --- 3. STRICT 5-COLUMN GRID (Headers) ---
            HorizontalDivider(thickness = 1.dp, color = dividerColor)
            Row(
                modifier = Modifier.fillMaxWidth().background(bgHeader).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("No.", Modifier.weight(1f), fontWeight = FontWeight.Black, color = textDarkGray, fontSize = 14.sp, fontFamily = latoFamily)
                Text("Item", Modifier.weight(3.5f), fontWeight = FontWeight.Black, color = textDarkGray, fontSize = 14.sp, fontFamily = latoFamily)
                Text("Qty", Modifier.weight(1.5f), fontWeight = FontWeight.Black, color = textDarkGray, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                Text("Rate", Modifier.weight(2f), fontWeight = FontWeight.Black, color = textDarkGray, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                Text("Total", Modifier.weight(2.5f), fontWeight = FontWeight.Black, color = textDarkGray, fontSize = 14.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
            }
            HorizontalDivider(thickness = 1.dp, color = dividerColor)

            // --- 4. HIGH-DENSITY, JUMBO DATA ROWS ---
            items.forEachIndexed { index, item ->
                val rowBg = if (index % 2 == 1) bgStripe else Color.White
                Row(
                    modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}", Modifier.weight(1f), color = textDarkGray, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = latoFamily)
                    Text(item.name, Modifier.weight(3.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = latoFamily)
                    Text("${item.quantity}", Modifier.weight(1.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                    Text(formatNumber(item.rate), Modifier.weight(2f), color = textDarkGray, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                    Text(formatNumber(item.total), Modifier.weight(2.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                }
            }

            // EXPERT: Stronger divider for Subtotal separation (gives it weight)
            HorizontalDivider(thickness = 2.dp, color = textDarkGray.copy(alpha = 0.3f))

            // --- 5. THE SILENT MATH GRID (Connecting the Dots Perfectly) ---
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(4.5f)) // Safely skips No. + Item

                // Under QTY Column (Slightly larger font to show subtotal weight)
                Text("${totalQuantity} Pcs", Modifier.weight(1.5f), fontWeight = FontWeight.Black, color = textBlack, fontSize = 19.sp, textAlign = TextAlign.End, fontFamily = latoFamily)

                Spacer(Modifier.weight(2f)) // Safely skips Rate

                // Under TOTAL Column
                Text("₹ ${formatNumber(subTotal)}", Modifier.weight(2.5f), fontWeight = FontWeight.Black, color = textBlack, fontSize = 19.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
            }

            // --- 6. SMART DISCOUNTS / CHARGES ---
            discounts.forEach { discount ->
                val isNegative = !discount.isPlus

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f)) // Skip No.
                    Text(discount.title, Modifier.weight(3.5f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = latoFamily)

                    val pctStr = if (discount.percentage > 0) "${discount.percentage}%" else ""
                    Text(pctStr, Modifier.weight(1.5f), color = textBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.End, fontFamily = latoFamily)

                    Spacer(Modifier.weight(2f)) // Skip Rate

                    val absoluteAmount = discount.amount.absoluteValue

                    // EXPERT: Payment/Discount Chip for negative, plain text for positive
                    if (isNegative) {
                        Box(modifier = Modifier.weight(2.5f), contentAlignment = Alignment.CenterEnd) {
                            Surface(color = chipGreenBg, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "- ₹ ${formatNumber(absoluteAmount)}",
                                    color = chipGreenText,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    fontFamily = latoFamily
                                )
                            }
                        }
                    } else {
                        // Plain text, NO '+' sign
                        Text("₹ ${formatNumber(absoluteAmount)}", Modifier.weight(2.5f), color = textBlack, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 7. THE MASTERPIECE PILL (Perfectly Balanced, No "Net Payable") ---
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColor)
                        .padding(horizontal = 20.dp, vertical = 20.dp), // Excellent breathing room
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exact requirement: "TOTAL : 16 PCS" in large font on a single line!
                    Text("TOTAL : ${totalQuantity} PCS", fontWeight = FontWeight.Black, color = footerTextColor, fontSize = 20.sp, fontFamily = latoFamily)

                    Spacer(Modifier.weight(1f))

                    Text("₹ ${formatNumber(grandTotal)}", fontWeight = FontWeight.Black, color = footerTextColor, fontSize = 34.sp, textAlign = TextAlign.End, fontFamily = latoFamily)
                }
            }
        }
    }
}