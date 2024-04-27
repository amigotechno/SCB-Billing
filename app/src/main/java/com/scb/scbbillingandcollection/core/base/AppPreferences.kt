package com.scb.scbbillingandcollection.core.base

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val preferences: SharedPreferences
) {
    companion object AppKeyConstants {
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val TOKEN = "token"
        const val USER_ID = "user_id"
        const val ROLE_ID = "role_id"
        const val NAME = "name"
        const val ENCRYPTED_FILE_NAME = "app_preferences"
    }

    var isLoggedIn: Boolean
        get() = preferences.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = preferences.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var token: String
        get() = preferences.getString(TOKEN, "") ?: ""
        set(value) = preferences.edit().putString(TOKEN, value).apply()

    var userId: String
        get() = preferences.getString(USER_ID, "") ?: ""
        set(value) = preferences.edit().putString(USER_ID, value).apply()

    var roleId: String
        get() = preferences.getString(ROLE_ID, "") ?: ""
        set(value) = preferences.edit().putString(ROLE_ID, value).apply()

    var name: String
        get() = preferences.getString(NAME, "") ?: ""
        set(value) = preferences.edit().putString(NAME, value).apply()

    fun clearPreferencesData() {
        preferences.edit().clear().apply()
    }
}



