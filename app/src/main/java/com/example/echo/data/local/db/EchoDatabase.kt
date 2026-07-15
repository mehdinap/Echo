package com.example.echo.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.echo.data.local.dao.ChatDao
import com.example.echo.data.local.dao.SearchHistoryDao
import com.example.echo.data.local.dao.SongDao
import com.example.echo.data.local.entity.*

@Database(
    entities = [SongEntity::class, SearchHistoryEntity::class, MessageEntity::class, ConversationEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun chatDao(): ChatDao

    companion object { const val NAME = "Echo.db" }
}
