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
    val isSaved: Boolean = false
)
