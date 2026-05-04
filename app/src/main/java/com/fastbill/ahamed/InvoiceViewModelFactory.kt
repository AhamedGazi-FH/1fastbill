package com.fastbill.ahamed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fastbill.ahamed.database.DiscountDao
import com.fastbill.ahamed.database.InvoiceRepository
import com.fastbill.ahamed.database.PreferencesRepository

class InvoiceViewModelFactory(
    private val invoiceRepository: InvoiceRepository? = null,
    private val preferencesRepository: PreferencesRepository,
    private val discountDao: DiscountDao? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(InvoiceViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                InvoiceViewModel(invoiceRepository!!, preferencesRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                SettingsViewModel(preferencesRepository, discountDao!!) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
