package com.usememos.android.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.usememos.android.core.AppContainer
import com.usememos.android.domain.util.buildMemoTags
import com.usememos.android.domain.util.extractTags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class FeedViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private data class FeedLocalState(
        val draftContent: String,
        val selectedVisibility: String,
        val selectedTag: String?,
        val searchQuery: String,
        val selectedDate: LocalDate?,
        val currentMonth: YearMonth,
        val isSaving: Boolean,
        val infoMessage: String?,
    )

    private val localState = MutableStateFlow(
        FeedLocalState(
            draftContent = "",
            selectedVisibility = "PRIVATE",
            selectedTag = null,
            searchQuery = "",
            selectedDate = null,
            currentMonth = YearMonth.now(),
            isSaving = false,
            infoMessage = null,
        ),
    )

    val uiState: StateFlow<FeedUiState> = combine(
        localState,
        container.observeMemosUseCase(),
    ) { local, memos ->
        val filteredMemos = memos.filter { memo ->
            val tagMatches = local.selectedTag?.let { tag -> tag in extractTags(memo.content) } ?: true
            val searchMatches = local.searchQuery.isBlank() ||
                memo.content.contains(local.searchQuery, ignoreCase = true)
            val memoDate = memo.displayTime.atZone(ZoneId.systemDefault()).toLocalDate()
            val dateMatches = local.selectedDate?.let { memoDate == it } ?: true
            tagMatches && searchMatches && dateMatches
        }
        FeedUiState(
            draftContent = local.draftContent,
            selectedVisibility = local.selectedVisibility,
            memos = filteredMemos,
            availableTags = buildMemoTags(memos),
            selectedTag = local.selectedTag,
            searchQuery = local.searchQuery,
            selectedDate = local.selectedDate,
            memoDates = memos.map { it.displayTime.atZone(ZoneId.systemDefault()).toLocalDate() }.toSet(),
            currentMonth = local.currentMonth,
            isSaving = local.isSaving,
            infoMessage = local.infoMessage,
            isConfigured = container.settingsStore.getBaseUrl().isNotBlank() &&
                container.settingsStore.getAccessToken().isNotBlank(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(),
    )

    init {
        syncNow()
    }

    fun onDraftChanged(value: String) {
        localState.value = localState.value.copy(draftContent = value)
    }

    fun onVisibilityChanged(value: String) {
        localState.value = localState.value.copy(selectedVisibility = value)
    }

    fun onTagSelected(value: String?) {
        localState.value = localState.value.copy(
            selectedTag = if (localState.value.selectedTag == value) null else value,
        )
    }

    fun onSearchQueryChanged(value: String) {
        localState.value = localState.value.copy(searchQuery = value)
    }

    fun onDateSelected(value: LocalDate?) {
        localState.value = localState.value.copy(
            selectedDate = if (localState.value.selectedDate == value) null else value,
        )
    }

    fun onMonthChanged(value: YearMonth) {
        localState.value = localState.value.copy(currentMonth = value)
    }

    fun saveMemo() {
        val content = localState.value.draftContent.trim()
        if (content.isBlank()) {
            localState.value = localState.value.copy(infoMessage = "Write something before saving.")
            return
        }

        viewModelScope.launch {
            localState.value = localState.value.copy(isSaving = true)
            container.createMemoUseCase(content, localState.value.selectedVisibility)
            localState.value = localState.value.copy(
                draftContent = "",
                infoMessage = "Saved locally. Sync queued.",
                isSaving = false,
            )
            container.syncScheduler.enqueueOneShotSync()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            localState.value = localState.value.copy(infoMessage = if (
                container.settingsStore.getBaseUrl().isBlank() ||
                container.settingsStore.getAccessToken().isBlank()
            ) {
                "Add your server URL and PAT before syncing."
            } else {
                "Syncing with your Memos server..."
            })
            container.syncScheduler.enqueueOneShotSync()
        }
    }
}

class FeedViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FeedViewModel(container) as T
}
