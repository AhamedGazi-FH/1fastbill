package com.fastbill.ahamed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fastbill.ahamed.database.InvoiceRepository
import com.fastbill.ahamed.database.PreferencesRepository

class InvoiceViewModelFactory(
    private val invoiceRepository: InvoiceRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InvoiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InvoiceViewModel(invoiceRepository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: Expected InvoiceViewModel")
    }
}
