package com.fastbill.ahamed.model

import com.fastbill.ahamed.database.Invoice
import com.fastbill.ahamed.database.Item

data class SharedBillPackage(
    val sharedId: String = "",
    val senderName: String = "",
    val timestamp: Long = 0L,
    val bill: Invoice = Invoice(),
    val billItems: List<Item> = emptyList()
)
