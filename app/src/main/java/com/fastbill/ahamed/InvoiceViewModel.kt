package com.fastbill.ahamed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.database.Invoice
import com.fastbill.ahamed.database.InvoiceRepository
import com.fastbill.ahamed.database.Item
import com.fastbill.ahamed.database.PreferencesRepository
import com.fastbill.ahamed.model.InvoiceUiState
import com.fastbill.ahamed.model.TemporaryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class InvoiceViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceUiState())
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    fun refreshPreferences() {
        val color = preferencesRepository.getSelectedColor()
        val isNumberOn = preferencesRepository.isShareNumberOn()
        val defaultNumber = preferencesRepository.getDefaultShareNumber() ?: ""
        val sn1 = preferencesRepository.getShareNumber(1)
        val sn2 = preferencesRepository.getShareNumber(2)
        val sn3 = preferencesRepository.getShareNumber(3)
        // Added Caption Preferences
        val isCapOn = preferencesRepository.isShareCaptionOn()
        val capTemp = preferencesRepository.getShareCaptionTemplate()

        _uiState.update { state ->
            state.copy(
                themeColor = color,
                isNumberOn = isNumberOn,
                defaultShareNumber = defaultNumber,
                shareNumber1 = sn1,
                shareNumber2 = sn2,
                shareNumber3 = sn3,
                isCaptionOn = isCapOn,
                captionTemplate = capTemp
            )
        }
    }

    fun loadInvoiceData(invoiceId: Int, adoptedItems: List<TemporaryItem> = emptyList(), adoptedDiscounts: List<Discount>? = null) {
        viewModelScope.launch {
            refreshPreferences()
            _uiState.update { it.copy(isLoading = true, invoiceId = invoiceId) }

            val initialDiscounts = if (invoiceId != 0) {
                invoiceRepository.getInitialDiscounts(invoiceId)
            } else {
                adoptedDiscounts ?: invoiceRepository.getInitialDiscounts(0)
            }

            val initialItems = if (invoiceId != 0) {
                invoiceRepository.getItemsForInvoice(invoiceId).map {
                    TemporaryItem(name = it.name, quantity = it.quantity, rate = it.rate, total = it.total)
                }
            } else {
                adoptedItems
            }

            val invoice = if (invoiceId != 0) invoiceRepository.getInvoiceById(invoiceId) else null
            val dateStr = convertTimestampToDate(invoice?.timestamp ?: System.currentTimeMillis())

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    discounts = initialDiscounts,
                    items = initialItems,
                    customerName = invoice?.name ?: state.customerName,
                    invoiceDate = dateStr
                )
            }
            calculateTotals()
        }
    }

    private fun calculateTotals() {
        _uiState.update { state ->
            val totalQuantity = state.items.sumOf { it.quantity }
            val subTotal = state.items.sumOf { it.total }

            var runningTotal = subTotal 

            val updatedDiscounts = state.discounts.sortedBy { it.orderIndex }.map { discount ->
                val absoluteAmount = if (discount.percentage > 0) {
                    runningTotal * (discount.percentage / 100.0)
                } else {
                    discount.price
                }

                if (discount.isPlus) {
                    runningTotal += absoluteAmount
                } else {
                    runningTotal -= absoluteAmount
                }

                discount.copy(amount = absoluteAmount)
            }

            state.copy(
                totalQuantity = totalQuantity,
                subTotal = subTotal,
                discounts = updatedDiscounts,
                grandTotal = runningTotal
            )
        }
    }

    fun getDefaultQuantity(): Int = preferencesRepository.getDefaultQuantity()

    fun addItem(item: TemporaryItem) {
        _uiState.update { it.copy(items = it.items + item) }
        calculateTotals()
    }

    fun updateItem(index: Int, newItem: TemporaryItem) {
        _uiState.update { state ->
            val newList = state.items.toMutableList()
            if (index in newList.indices) newList[index] = newItem
            state.copy(items = newList)
        }
        calculateTotals()
    }

    fun removeItem(index: Int) {
        _uiState.update { state ->
            val newList = state.items.toMutableList()
            if (index in newList.indices) newList.removeAt(index)
            state.copy(items = newList)
        }
        calculateTotals()
    }

    fun addDiscount(discount: Discount) {
        _uiState.update { state ->
            if (state.discounts.none { it.id == discount.id }) {
                state.copy(discounts = state.discounts + discount.copy(isActive = true))
            } else state
        }
        calculateTotals()
    }

    fun updateDiscount(index: Int, newDiscount: Discount) {
        _uiState.update { state ->
            val newList = state.discounts.toMutableList()
            if (index in newList.indices) newList[index] = newDiscount
            state.copy(discounts = newList)
        }
        calculateTotals()
    }

    fun removeDiscount(discountId: Int) {
        _uiState.update { state -> state.copy(discounts = state.discounts.filter { it.id != discountId }) }
        calculateTotals()
    }

    fun removeDiscountAt(index: Int) {
        _uiState.update { state ->
            val newList = state.discounts.toMutableList()
            if (index in newList.indices) newList.removeAt(index)
            state.copy(discounts = newList)
        }
        calculateTotals()
    }

    fun swapDiscounts(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val newList = state.discounts.toMutableList()
            if (fromIndex in newList.indices && toIndex in newList.indices) {
                Collections.swap(newList, fromIndex, toIndex)
                newList.forEachIndexed { index, discount ->
                    newList[index] = discount.copy(orderIndex = index)
                }
            }
            state.copy(discounts = newList)
        }
        calculateTotals()
    }
    
    fun setCustomerName(name: String) {
        _uiState.update { it.copy(customerName = name) }
    }

    fun saveInvoice() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val invoice = Invoice(invoiceId = currentState.invoiceId, name = currentState.customerName, timestamp = System.currentTimeMillis(), total = currentState.grandTotal)
            val items = currentState.items.map { Item(name = it.name, quantity = it.quantity, rate = it.rate, total = it.total, invoiceId = currentState.invoiceId) }
            val newInvoiceId = invoiceRepository.saveFullInvoice(invoice, items, currentState.discounts)
            _uiState.update { it.copy(invoiceId = newInvoiceId, isSaved = true) }
        }
    }
    
    fun resetSaveState() {
        _uiState.update { it.copy(isSaved = false) }
    }

    private fun convertTimestampToDate(timestamp: Long): String = 
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(timestamp))
}
