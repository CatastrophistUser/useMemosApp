package com.usememos.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.usememos.android.domain.model.Memo
import java.time.Instant
import java.util.UUID

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey val id: String,
    val remoteName: String?,
    val content: String,
    val visibility: String,
    val createdAt: Long,
    val updatedAt: Long,
    val displayTime: Long,
    val syncState: SyncState,
    val errorMessage: String?,
) {
    fun toDomain(): Memo = Memo(
        id = id,
        remoteName = remoteName,
        content = content,
        visibility = visibility,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        displayTime = Instant.ofEpochMilli(displayTime),
        syncState = syncState,
        errorMessage = errorMessage,
    )

    companion object {
        fun localDraft(
            content: String,
            visibility: String,
            nowMillis: Long = System.currentTimeMillis(),
        ): MemoEntity = MemoEntity(
            id = UUID.randomUUID().toString(),
            remoteName = null,
            content = content,
            visibility = visibility,
            createdAt = nowMillis,
            updatedAt = nowMillis,
            displayTime = nowMillis,
            syncState = SyncState.PENDING,
            errorMessage = null,
        )
    }
}
