package com.usememos.android.data.repository

import android.util.Log
import com.usememos.android.data.local.MemoDao
import com.usememos.android.data.local.MemoEntity
import com.usememos.android.data.local.SyncState
import com.usememos.android.data.remote.MemosApiClient
import com.usememos.android.domain.model.Memo
import com.usememos.android.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultMemoRepository(
    private val memoDao: MemoDao,
    private val apiClient: MemosApiClient,
) : MemoRepository {
    override fun observeMemos(): Flow<List<Memo>> = memoDao.observeAll().map { memos ->
        memos.map(MemoEntity::toDomain)
    }

    override suspend fun createLocalMemo(content: String, visibility: String) {
        memoDao.insert(MemoEntity.localDraft(content = content, visibility = visibility))
    }

    override suspend fun syncPendingMemos(): Result<Unit> = runCatching {
        val pending = memoDao.getBySyncState(SyncState.PENDING) + memoDao.getBySyncState(SyncState.FAILED)
        Log.d(TAG, "syncPendingMemos pendingCount=${pending.size}")
        pending.forEach { memo ->
            val syncedMemo = apiClient.syncPendingMemo(memo).getOrElse { throwable ->
                memoDao.update(
                    memo.copy(
                        syncState = SyncState.FAILED,
                        errorMessage = throwable.message,
                    ),
                )
                throw throwable
            }
            if (syncedMemo.id != memo.id) {
                memoDao.deleteById(memo.id)
            }
            memoDao.insert(
                syncedMemo.copy(
                    id = syncedMemo.remoteName ?: memo.id,
                    syncState = SyncState.SYNCED,
                    errorMessage = null,
                ),
            )
        }

        val remoteMemos = apiClient.fetchMemos().getOrThrow()
        Log.d(TAG, "syncPendingMemos remoteCount=${remoteMemos.size}")
        
        // Remove local memos that are no longer present on the server
        val remoteIds = remoteMemos.map { it.id }.toSet()
        val localSynced = memoDao.getBySyncState(SyncState.SYNCED)
        localSynced.forEach { local ->
            if (!remoteIds.contains(local.id)) {
                Log.d(TAG, "syncPendingMemos deleting removed memo id=${local.id}")
                memoDao.deleteById(local.id)
            }
        }

        memoDao.upsertAll(remoteMemos)
    }

    companion object {
        private const val TAG = "DefaultMemoRepository"
    }
}
