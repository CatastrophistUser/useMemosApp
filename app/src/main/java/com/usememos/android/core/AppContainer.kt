package com.usememos.android.core

import android.content.Context
import androidx.room.Room
import com.usememos.android.data.local.MemosDatabase
import com.usememos.android.data.remote.MemosApiClient
import com.usememos.android.data.repository.DefaultMemoRepository
import com.usememos.android.data.settings.SecureSettingsStore
import com.usememos.android.domain.repository.MemoRepository
import com.usememos.android.domain.usecase.CreateMemoUseCase
import com.usememos.android.domain.usecase.ObserveMemosUseCase
import com.usememos.android.domain.usecase.SyncPendingMemosUseCase
import com.usememos.android.sync.SyncScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsStore = SecureSettingsStore(appContext)

    private val database = Room.databaseBuilder(
        appContext,
        MemosDatabase::class.java,
        "memos.db",
    ).fallbackToDestructiveMigration().build()

    private val apiClient = MemosApiClient(settingsStore)

    val memoRepository: MemoRepository = DefaultMemoRepository(
        memoDao = database.memoDao(),
        apiClient = apiClient,
    )

    val observeMemosUseCase = ObserveMemosUseCase(memoRepository)
    val createMemoUseCase = CreateMemoUseCase(memoRepository)
    val syncPendingMemosUseCase = SyncPendingMemosUseCase(memoRepository)
    val syncScheduler = SyncScheduler(appContext)
}
