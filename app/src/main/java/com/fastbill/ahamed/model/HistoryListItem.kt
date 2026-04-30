package com.fastbill.ahamed.model

import com.fastbill.ahamed.database.Invoice

sealed class HistoryListItem {
    data class DateHeader(
        val date: String,
        val billCount: Int,
        val dailyTotal: Double
    ) : HistoryListItem()

    data class BillData(
        val invoice: Invoice
    ) : HistoryListItem()
}
