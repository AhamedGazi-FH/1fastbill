package com.fastbill.ahamed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.model.TemporaryItem
import com.fastbill.ahamed.templates.TemplateClassic
import com.fastbill.ahamed.templates.TemplateModern
import com.fastbill.ahamed.templates.TemplatePremium
import com.fastbill.ahamed.templates.TemplateStandard

// ==========================================================
// 👑 MASTER ROUTER (Switches Design Instantly)
// ==========================================================
@Composable
fun InvoicePrintTemplate(
    customerName: String,
    date: String,
    items: List<TemporaryItem>,
    discounts: List<Discount>,
    totalQuantity: Int,
    subTotal: Double,
    grandTotal: Double,
    themeColorHex: String,
    selectedDesign: String
) {
    val currentDensity = LocalDensity.current

    // UNIVERSAL SCALING LOCK: Keeps UI exact regardless of phone settings
    CompositionLocalProvider(LocalDensity provides Density(density = currentDensity.density, fontScale = 1f)) {
        when (selectedDesign) {
            "Modern" -> TemplateModern(customerName, date, items, discounts, totalQuantity, subTotal, grandTotal, themeColorHex)
            "Classic" -> TemplateClassic(customerName, date, items, discounts, totalQuantity, subTotal, grandTotal, themeColorHex)
            "Premium" -> TemplatePremium(customerName, date, items, discounts, totalQuantity, subTotal, grandTotal, themeColorHex)
            "Standard" -> TemplateStandard(customerName, date, items, discounts, totalQuantity, subTotal, grandTotal, themeColorHex)
            else -> TemplateModern(customerName, date, items, discounts, totalQuantity, subTotal, grandTotal, themeColorHex)
        }
    }
}