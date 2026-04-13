package com.usememos.android.ui.feed

import com.usememos.android.domain.model.Memo
import com.usememos.android.domain.model.MemoTag
import java.time.LocalDate
import java.time.YearMonth

data class FeedUiState(
    val draftContent: String = "",
    val selectedVisibility: String = "PRIVATE",
    val memos: List<Memo> = emptyList(),
    val availableTags: List<MemoTag> = emptyList(),
    val selectedTag: String? = null,
    val searchQuery: String = "",
    val selectedDate: LocalDate? = null,
    val memoDates: Set<LocalDate> = emptySet(),
    val currentMonth: YearMonth = YearMonth.now(),
    val isSaving: Boolean = false,
    val infoMessage: String? = null,
    val isConfigured: Boolean = false,
)
