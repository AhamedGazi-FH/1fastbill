package com.fastbill.ahamed.model

import com.fastbill.ahamed.database.Discount

data class InvoiceUiState(
    val invoiceId: Int = 0,
    val isLoading: Boolean = false,
    val items: List<TemporaryItem> = emptyList(),
    val discounts: List<Discount> = emptyList(),
    val totalQuantity: Int = 0,
    val subTotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val customerName: String = "",
    val invoiceDate: String = "",
    val isSaved: Boolean = false,
    val themeColor: String = "#6750A4",
    val isNumberOn: Boolean = false,
    val defaultShareNumber: String = "",
    val shareNumber1: String = "",
    val shareNumber2: String = "",
    val shareNumber3: String = "",
    val shareApp: String = "other",
    val isCaptionOn: Boolean = true,
    val captionTemplate: String = ""
)
