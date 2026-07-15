package com.example.echo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.echo.domain.model.*

/** Cached catalog. `localFilePath` is non-null once WorkManager finishes a download. */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artistId: Long,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val genre: String?,
    val isLocal: Boolean,
    val localFilePath: String? = null,
    val downloadedAt: Long? = null,
    val isLiked: Boolean = false,
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long,
)

@Entity(
    tableName = "messages",
    indices = [Index("conversationId"), Index(value = ["clientId"], unique = true)],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Long?,
    val clientId: String?,
    val conversationId: Long,
    val senderId: Long,
    val body: String?,
    val songId: Long?,
    val status: String,
    val createdAt: Long,
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: Long,
    val peerId: Long,
    val peerName: String,
    val peerEchotar: String?,
    val unreadCount: Int,
    val updatedAt: Long,
)

// ---- mappers ----

fun SongEntity.toDomain() = Song(
    id, title, artistId, artistName, coverImageUrl, audioUrl, durationMs, genre, isLocal, localFilePath
)

fun Song.toEntity(liked: Boolean = false, path: String? = localFilePath) = SongEntity(
    id, title, artistId, artistName, coverImageUrl, audioUrl, durationMs, genre, isLocal,
    localFilePath = path, downloadedAt = if (path != null) System.currentTimeMillis() else null,
    isLiked = liked,
)

fun MessageEntity.toDomain(song: Song? = null) = Message(
    id = serverId ?: localId,
    clientId = clientId,
    conversationId = conversationId,
    senderId = senderId,
    body = body,
    song = song,
    status = if (serverId == null) MessageStatus.SENDING else MessageStatus.from(status),
    createdAt = createdAt,
)

fun Message.toEntity() = MessageEntity(
    serverId = if (status == MessageStatus.SENDING) null else id,
    clientId = clientId,
    conversationId = conversationId,
    senderId = senderId,
    body = body,
    songId = song?.id,
    status = status.name,
    createdAt = createdAt,
)
