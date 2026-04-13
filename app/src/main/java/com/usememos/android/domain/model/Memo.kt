package com.usememos.android.domain.model

import com.usememos.android.data.local.SyncState
import java.time.Instant

data class Memo(
    val id: String,
    val remoteName: String?,
    val content: String,
    val visibility: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val displayTime: Instant,
    val syncState: SyncState,
    val errorMessage: String?,
)
