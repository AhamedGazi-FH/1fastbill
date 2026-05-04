package com.fastbill.ahamed.model

import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.database.Item

object SharedDataHolder {
    var itemsToAdopt: List<Item>? = null
    var discountsToAdopt: List<Discount>? = null // Added discounts
}
