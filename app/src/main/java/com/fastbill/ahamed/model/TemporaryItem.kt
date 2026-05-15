package com.fastbill.ahamed.model

data class TemporaryItem(
    val name: String,
    val quantity: Int,
    val rate: Double,
    val total: Double,
    val id: String = java.util.UUID.randomUUID().toString()
)