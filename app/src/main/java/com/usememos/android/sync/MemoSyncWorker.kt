package com.usememos.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.usememos.android.MemosApplication

/**
 * Pushes local drafts to Memos and then refreshes the timeline.
 *
 * The worker treats host lookup and timeout failures as retryable because
 * a Tailscale-backed server can briefly appear unavailable while the tunnel
 * wakes up or re-establishes peer connectivity.
 */
class MemoSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as MemosApplication).container
        return container.syncPendingMemosUseCase().fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
