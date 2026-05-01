package com.fastbill.ahamed.model

import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.database.Invoice
import com.fastbill.ahamed.database.Item

data class AppBackupSnapshot(
    val profileName: String = "",
    val timestamp: Long = 0L,
    val billsList: List<Invoice> = emptyList(),
    val billItemsList: List<Item> = emptyList(),
    val discountList: List<Discount> = emptyList(),
    val settingsMap: Map<String, Any?> = emptyMap()
)
