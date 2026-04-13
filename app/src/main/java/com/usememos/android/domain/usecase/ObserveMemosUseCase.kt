package com.usememos.android.domain.usecase

import com.usememos.android.domain.repository.MemoRepository

class ObserveMemosUseCase(
    private val repository: MemoRepository,
) {
    operator fun invoke() = repository.observeMemos()
}
