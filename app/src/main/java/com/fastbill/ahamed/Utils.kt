package com.fastbill.ahamed

import android.content.Context

object Utils {
    private const val ACTIVATION_CODE_HASH = "0387b37267f50a41738739660893f4129b01083e91143899201a09df28e21966"

    fun isValidActivationCode(input: String): Boolean {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray())
            val inputHash = hash.joinToString("") { "%02x".format(it) }
            inputHash == ACTIVATION_CODE_HASH
        } catch (e: Exception) {
            false
        }
    }

    // Get status bar height dynamically
    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    // Get navigation bar height dynamically
    fun getNavigationBarHeight(context: Context): Int {
        val resourceId =
            context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }
}