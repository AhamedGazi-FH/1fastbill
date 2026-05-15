package com.fastbill.ahamed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fastbill.ahamed.database.DiscountDao
import com.fastbill.ahamed.database.PreferencesRepository

class SettingsViewModelFactory(
    private val preferencesRepository: PreferencesRepository,
    private val discountDao: DiscountDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(preferencesRepository, discountDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: Expected SettingsViewModel")
    }
}
