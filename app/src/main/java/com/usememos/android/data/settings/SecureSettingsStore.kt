package com.usememos.android.data.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureSettingsStore(context: Context) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getBaseUrl(): String = sharedPreferences.getString(KEY_BASE_URL, "").orEmpty()

    fun getAccessToken(): String = sharedPreferences.getString(KEY_ACCESS_TOKEN, "").orEmpty()

    fun saveConnection(baseUrl: String, accessToken: String) {
        sharedPreferences.edit()
            .putString(KEY_BASE_URL, baseUrl.trim())
            .putString(KEY_ACCESS_TOKEN, accessToken.trim())
            .apply()
    }

    companion object {
        private const val FILE_NAME = "secure_connection_prefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
