package com.usememos.android.domain.repository

import com.usememos.android.domain.model.Memo
import kotlinx.coroutines.flow.Flow

interface MemoRepository {
    fun observeMemos(): Flow<List<Memo>>
    suspend fun createLocalMemo(content: String, visibility: String)
    suspend fun syncPendingMemos(): Result<Unit>
}
