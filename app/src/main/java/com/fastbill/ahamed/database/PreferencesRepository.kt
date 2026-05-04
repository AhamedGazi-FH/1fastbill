package com.fastbill.ahamed.database

import android.content.SharedPreferences

class PreferencesRepository(private val sharedPreferences: SharedPreferences) {

    companion object {
        const val KEY_DEFAULT_QTY = "default_quantity"
        const val KEY_SELECTED_COLOR = "selected_color_code"
        const val KEY_SHARE_CAPTION = "share_caption"
        const val KEY_SHARE_CAPTION_TEMPLATE = "share_caption_template"
        const val KEY_SHARE_APP = "share_app"
        const val KEY_SHARE_NUMBER_ON = "share_number_on"
        const val KEY_SHARE_NUMBER_1 = "share_number_1"
        const val KEY_SHARE_NUMBER_2 = "share_number_2"
        const val KEY_SHARE_NUMBER_3 = "share_number_3"
        const val KEY_DEFAULT_SHARE_NUMBER = "default_share_number"
        const val KEY_AUTO_BACKUP_DAYS = "auto_backup_days"
        const val KEY_DEFAULT_BACKUP_NAME = "default_backup_name"
        const val KEY_COLORS = "colors"
        const val KEY_SETTINGS_JUST_UPDATED = "settings_just_updated"

        private const val DEFAULT_QTY = 4
        private const val DEFAULT_COLOR = "#6750A4"
        private const val DEFAULT_CAPTION_TEMPLATE = "*Total {qty} Pcs {total}*"
        private const val DEFAULT_SHARE_APP = "other"
    }

    fun getDefaultQuantity(): Int = sharedPreferences.getInt(KEY_DEFAULT_QTY, DEFAULT_QTY)
    fun setDefaultQuantity(quantity: Int) = sharedPreferences.edit().putInt(KEY_DEFAULT_QTY, quantity).apply()

    fun getSelectedColor(): String = sharedPreferences.getString(KEY_SELECTED_COLOR, DEFAULT_COLOR) ?: DEFAULT_COLOR
    fun setSelectedColor(color: String) = sharedPreferences.edit().putString(KEY_SELECTED_COLOR, color).apply()

    fun isShareCaptionOn(): Boolean = sharedPreferences.getBoolean(KEY_SHARE_CAPTION, true)
    fun setShareCaptionOn(on: Boolean) = sharedPreferences.edit().putBoolean(KEY_SHARE_CAPTION, on).apply()

    fun getShareCaptionTemplate(): String = sharedPreferences.getString(KEY_SHARE_CAPTION_TEMPLATE, DEFAULT_CAPTION_TEMPLATE) ?: DEFAULT_CAPTION_TEMPLATE
    fun setShareCaptionTemplate(template: String) = sharedPreferences.edit().putString(KEY_SHARE_CAPTION_TEMPLATE, template).apply()

    fun getShareAppPreference(): String = sharedPreferences.getString(KEY_SHARE_APP, DEFAULT_SHARE_APP) ?: DEFAULT_SHARE_APP
    fun setShareAppPreference(preference: String) = sharedPreferences.edit().putString(KEY_SHARE_APP, preference).apply()

    fun isShareNumberOn(): Boolean = sharedPreferences.getBoolean(KEY_SHARE_NUMBER_ON, false)
    fun setShareNumberOn(on: Boolean) = sharedPreferences.edit().putBoolean(KEY_SHARE_NUMBER_ON, on).apply()

    fun getShareNumber(index: Int): String = sharedPreferences.getString("share_number_$index", "") ?: ""
    fun setShareNumber(index: Int, number: String) = sharedPreferences.edit().putString("share_number_$index", number).apply()

    fun getDefaultShareNumber(): String? = sharedPreferences.getString(KEY_DEFAULT_SHARE_NUMBER, null)
    fun setDefaultShareNumber(number: String) = sharedPreferences.edit().putString(KEY_DEFAULT_SHARE_NUMBER, number).apply()

    fun getAutoBackupDays(): Int = sharedPreferences.getInt(KEY_AUTO_BACKUP_DAYS, 10)
    fun setAutoBackupDays(days: Int) = sharedPreferences.edit().putInt(KEY_AUTO_BACKUP_DAYS, days).apply()

    fun getDefaultBackupName(): String? = sharedPreferences.getString(KEY_DEFAULT_BACKUP_NAME, null)

    fun getColors(): List<String> {
        val savedColors = sharedPreferences.getStringSet(KEY_COLORS, null)
        return if (savedColors.isNullOrEmpty()) {
            listOf("#6750A4", "#3a0ca3", "#FEE440", "#4361ee")
        } else {
            savedColors.toList().reversed()
        }
    }

    fun setColors(colors: List<String>) {
        sharedPreferences.edit().putStringSet(KEY_COLORS, colors.toSet()).apply()
    }

    fun setSettingsJustUpdated(updated: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SETTINGS_JUST_UPDATED, updated).apply()
    }
}
