package com.usememos.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MemoEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(RoomConverters::class)
abstract class MemosDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
}
