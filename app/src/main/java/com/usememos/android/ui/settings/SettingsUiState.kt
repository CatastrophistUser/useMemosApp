package com.usememos.android.ui.settings

data class SettingsUiState(
    val baseUrl: String = "",
    val accessToken: String = "",
    val isSaving: Boolean = false,
    val infoMessage: String? = null,
    val saveSucceeded: Boolean = false,
)
