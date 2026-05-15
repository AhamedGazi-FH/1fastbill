package com.fastbill.ahamed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastbill.ahamed.database.Discount
import com.fastbill.ahamed.database.DiscountDao
import com.fastbill.ahamed.database.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val shareApp: String = "other",
    val isCaptionOn: Boolean = true,
    val captionTemplate: String = "",
    val isNumberOn: Boolean = false,
    val number1: String = "",
    val number2: String = "",
    val number3: String = "",
    val backupDays: Int = 10,
    val defaultQuantity: Int = 4,
    val selectedColor: String = "#6750A4",
    val colorList: List<String> = emptyList(),
    val defaultBackupName: String? = null,
    val discounts: List<Discount> = emptyList(),
    val isLoaded: Boolean = false
)

class SettingsViewModel(
    private val prefsRepo: PreferencesRepository,
    private val discountDao: DiscountDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun refreshSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedState = _uiState.value.copy(
                selectedColor = prefsRepo.getSelectedColor(),
                defaultQuantity = prefsRepo.getDefaultQuantity(),
                shareApp = prefsRepo.getShareAppPreference(),
                isCaptionOn = prefsRepo.isShareCaptionOn(),
                captionTemplate = prefsRepo.getShareCaptionTemplate(),
                isNumberOn = prefsRepo.isShareNumberOn(),
                number1 = prefsRepo.getShareNumber(1),
                number2 = prefsRepo.getShareNumber(2),
                number3 = prefsRepo.getShareNumber(3),
                backupDays = prefsRepo.getAutoBackupDays()
            )
            withContext(Dispatchers.Main) {
                _uiState.value = updatedState
            }
            prefsRepo.setSettingsJustUpdated(true)
        }
    }

    private fun loadAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            val discounts = discountDao.getAllDiscountsSorted()
            val colors = prefsRepo.getColors()
            _uiState.update { state ->
                state.copy(
                    discounts = discounts,
                    colorList = colors,
                    shareApp = prefsRepo.getShareAppPreference(),
                    isCaptionOn = prefsRepo.isShareCaptionOn(),
                    captionTemplate = prefsRepo.getShareCaptionTemplate(),
                    isNumberOn = prefsRepo.isShareNumberOn(),
                    number1 = prefsRepo.getShareNumber(1),
                    number2 = prefsRepo.getShareNumber(2),
                    number3 = prefsRepo.getShareNumber(3),
                    backupDays = prefsRepo.getAutoBackupDays(),
                    defaultQuantity = prefsRepo.getDefaultQuantity(),
                    selectedColor = prefsRepo.getSelectedColor(),
                    defaultBackupName = prefsRepo.getDefaultBackupName(),
                    isLoaded = true
                )
            }
        }
    }

    fun refreshDiscounts() {
        viewModelScope.launch(Dispatchers.IO) {
            val discounts = discountDao.getAllDiscountsSorted()
            _uiState.update { it.copy(discounts = discounts) }
            prefsRepo.setSettingsJustUpdated(true)
        }
    }
}
