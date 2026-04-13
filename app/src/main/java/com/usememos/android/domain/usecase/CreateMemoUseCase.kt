package com.usememos.android.domain.usecase

import com.usememos.android.domain.repository.MemoRepository

class CreateMemoUseCase(
    private val repository: MemoRepository,
) {
    suspend operator fun invoke(content: String, visibility: String = "PRIVATE") {
        repository.createLocalMemo(content.trim(), visibility)
    }
}
