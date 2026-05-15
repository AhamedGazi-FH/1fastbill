package com.fastbill.ahamed.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val itemDao: ItemDao,
    private val discountDao: DiscountDao,
    private val invoiceDiscountDao: InvoiceDiscountDao
) {

    suspend fun getInvoiceById(invoiceId: Int): Invoice? {
        return invoiceDao.getInvoiceById(invoiceId)
    }

    suspend fun getItemsForInvoice(invoiceId: Int): List<Item> {
        return itemDao.getItemsForInvoice(invoiceId)
    }

    suspend fun getInitialDiscounts(invoiceId: Int): List<Discount> {
        return if (invoiceId == 0) {
            discountDao.getAllDiscountsSorted()
                .filter { it.isActive }
                .map { it.copy() }
        } else {
            invoiceDiscountDao.getDiscountsForInvoice(invoiceId).map { invDiscount ->
                Discount(
                    id = invDiscount.discountId,
                    title = invDiscount.title,
                    percentage = invDiscount.percentage,
                    price = invDiscount.price,
                    isPlus = invDiscount.isPlus,
                    isActive = true,
                    invoiceId = invDiscount.invoiceId,
                    orderIndex = invDiscount.orderIndex
                )
            }
        }
    }

    suspend fun insertFullInvoice(
        invoice: Invoice,
        items: List<Item>,
        discounts: List<Discount>
    ): Long = withContext(Dispatchers.IO) {
        val finalInvoiceId = invoiceDao.insert(invoice).toInt()

        val itemsToSave = items.map { it.copy(invoiceId = finalInvoiceId) }
        itemDao.insertAll(itemsToSave)

        saveDiscountsForInvoice(finalInvoiceId, discounts)

        return@withContext finalInvoiceId.toLong()
    }

    suspend fun updateFullInvoice(
        invoice: Invoice,
        items: List<Item>,
        discounts: List<Discount>
    ) = withContext(Dispatchers.IO) {
        invoiceDao.updateInvoice(invoice)
        itemDao.deleteItemsForInvoice(invoice.invoiceId)
        invoiceDiscountDao.deleteByInvoiceId(invoice.invoiceId)

        val itemsToSave = items.map { it.copy(invoiceId = invoice.invoiceId) }
        itemDao.insertAll(itemsToSave)

        saveDiscountsForInvoice(invoice.invoiceId, discounts)
    }

    private suspend fun saveDiscountsForInvoice(invoiceId: Int, discounts: List<Discount>) {
        discounts.forEach { discount ->
            val invoiceDiscount = InvoiceDiscount(
                discountId = discount.id,
                invoiceId = invoiceId,
                title = discount.title,
                percentage = discount.percentage,
                price = discount.price,
                isPlus = discount.isPlus,
                isActive = discount.isActive,
                orderIndex = discount.orderIndex
            )
            invoiceDiscountDao.insert(invoiceDiscount)
        }
    }

    @Deprecated("Use insertFullInvoice or updateFullInvoice instead", ReplaceWith("if (invoice.invoiceId == 0) insertFullInvoice(invoice, items, discounts) else updateFullInvoice(invoice, items, discounts)"))
    suspend fun saveFullInvoice(
        invoice: Invoice,
        items: List<Item>,
        discounts: List<Discount>
    ): Int = withContext(Dispatchers.IO) {
        val finalInvoiceId = if (invoice.invoiceId == 0) {
            invoiceDao.insert(invoice).toInt()
        } else {
            invoiceDao.updateInvoice(invoice)
            itemDao.deleteItemsForInvoice(invoice.invoiceId)
            invoiceDiscountDao.deleteByInvoiceId(invoice.invoiceId)
            invoice.invoiceId
        }

        val itemsToSave = items.map { it.copy(invoiceId = finalInvoiceId) }
        itemDao.insertAll(itemsToSave)

        saveDiscountsForInvoice(finalInvoiceId, discounts)

        return@withContext finalInvoiceId
    }
}
