package com.example.echo.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.example.echo.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity)

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun byId(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY title")
    fun likedSongs(): Flow<List<SongEntity>>

    @Query("UPDATE songs SET isLiked = :liked WHERE id = :id")
    suspend fun setLiked(id: Long, liked: Boolean)

    // ---- downloads ----
    @Query("SELECT * FROM songs WHERE localFilePath IS NOT NULL ORDER BY downloadedAt DESC")
    fun downloadsByDate(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE localFilePath IS NOT NULL ORDER BY title")
    fun downloadsByTitle(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE localFilePath IS NOT NULL ORDER BY artistName")
    fun downloadsByArtist(): Flow<List<SongEntity>>

    @Query("UPDATE songs SET localFilePath = :path, downloadedAt = :at WHERE id = :id")
    suspend fun setLocalPath(id: Long, path: String?, at: Long?)

    @Query("SELECT localFilePath FROM songs WHERE id = :id")
    suspend fun localPath(id: Long): String?
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 10")
    fun history(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun remove(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}

@Dao
interface ChatDao {
    /** Room-backed PagingSource: chat history pages straight out of the database. */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC")
    fun pagedMessages(conversationId: Long): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT 1")
    fun lastMessage(conversationId: Long): Flow<MessageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE clientId = :clientId LIMIT 1")
    suspend fun byClientId(clientId: String): MessageEntity?

    @Query("UPDATE messages SET serverId = :serverId, status = :status WHERE clientId = :clientId")
    suspend fun confirm(clientId: String, serverId: Long, status: String)

    @Query("UPDATE messages SET status = :status WHERE conversationId = :conversationId AND senderId = :senderId")
    suspend fun updateStatusForSender(conversationId: Long, senderId: Long, status: String)

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun conversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversations(items: List<ConversationEntity>)
}
