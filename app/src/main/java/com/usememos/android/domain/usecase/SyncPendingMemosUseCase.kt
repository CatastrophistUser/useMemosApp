package com.usememos.android.domain.usecase

import com.usememos.android.domain.repository.MemoRepository

class SyncPendingMemosUseCase(
    private val repository: MemoRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.syncPendingMemos()
}
