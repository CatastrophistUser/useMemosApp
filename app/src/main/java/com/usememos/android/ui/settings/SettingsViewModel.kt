package com.usememos.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.usememos.android.core.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            baseUrl = container.settingsStore.getBaseUrl(),
            accessToken = container.settingsStore.getAccessToken(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onBaseUrlChanged(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value, saveSucceeded = false)
    }

    fun onTokenChanged(value: String) {
        _uiState.value = _uiState.value.copy(accessToken = value, saveSucceeded = false)
    }

    fun save() {
        val baseUrl = _uiState.value.baseUrl.trim()
        val accessToken = _uiState.value.accessToken.trim()
        if (baseUrl.isBlank() || accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                infoMessage = "Enter both the server URL and personal access token.",
                saveSucceeded = false,
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                infoMessage = "Saving connection and fetching memos...",
                saveSucceeded = false,
            )

            container.settingsStore.saveConnection(
                baseUrl = baseUrl,
                accessToken = accessToken,
            )

            container.syncPendingMemosUseCase().fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        infoMessage = "Connection saved. Sync finished. Return to the feed to confirm your memos loaded.",
                        saveSucceeded = true,
                    )
                },
                onFailure = { throwable ->
                    container.syncScheduler.enqueueOneShotSync()
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        infoMessage = throwable.message ?: "Sync failed. Background retry queued.",
                        saveSucceeded = false,
                    )
                },
            )
        }
    }

    fun clearSaveSucceeded() {
        _uiState.value = _uiState.value.copy(saveSucceeded = false)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}

class SettingsViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(container) as T
}
