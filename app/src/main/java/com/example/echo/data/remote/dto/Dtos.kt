package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.echo.domain.model.*

@Serializable
data class PageDto<T>(
    val items: List<T>,
    val page: Int,
    val hasNext: Boolean,
)

@Serializable
data class SongDto(
    val id: Long,
    val title: String,
    val artistId: Long = 0,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val durationMs: Long = 0,
    val genre: String? = null,
    val isLocal: Boolean = false,
)

@Serializable
data class ArtistDto(val id: Long, val name: String, val imageUrl: String? = null, val followers: Int = 0)

@Serializable
data class PlaylistDto(
    val id: Long,
    val title: String,
    val coverUrl: String? = null,
    val ownerId: Long? = null,
    val ownerName: String? = null,
    val isPublic: Boolean = true,
    val kind: String = "user",
    val songCount: Int = 0,
    val songs: List<SongDto> = emptyList(),
)

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isPremium: Boolean = false,
    val isFollowing: Boolean = false,
    val followers: Int = 0,
    val publicPlaylists: List<PlaylistDto> = emptyList(),
)

@Serializable data class AuthRequest(val username: String, val password: String, val displayName: String? = null)
@Serializable data class AuthResponse(val token: String, val user: UserDto)

@Serializable
data class MessageDto(
    val id: Long,
    val clientId: String? = null,
    val conversationId: Long,
    val senderId: Long,
    val body: String? = null,
    val songId: Long? = null,
    val song: SongDto? = null,
    val status: String = "SENT",
    val createdAt: Long,
)

@Serializable
data class ConversationDto(
    val id: Long,
    val peer: UserDto,
    val lastMessage: MessageDto? = null,
    val unreadCount: Int = 0,
)

@Serializable data class ConversationIdDto(val id: Long)
@Serializable
data class ProfileUpdateDto(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val avatarData: String? = null,
    val avatarMimeType: String? = null,
)

// ---- mappers ---------------------------------------------------------------

fun SongDto.toDomain(localPath: String? = null) = Song(
    id, title, artistId, artistName, coverImageUrl, audioUrl, durationMs, genre, isLocal, localPath
)

fun ArtistDto.toDomain() = Artist(id, name, imageUrl, followers)

fun PlaylistDto.toDomain() = Playlist(
    id = id, title = title, coverUrl = coverUrl, ownerId = ownerId, ownerName = ownerName,
    kind = PlaylistKind.from(kind), songCount = songCount, songs = songs.map { it.toDomain() },
)

fun UserDto.toDomain() = User(
    id, username, displayName, avatarUrl, isPremium, isFollowing, followers,
    publicPlaylists.map { it.toDomain() },
)

fun MessageDto.toDomain() = Message(
    id = id, clientId = clientId, conversationId = conversationId, senderId = senderId,
    body = body, song = song?.toDomain(), status = MessageStatus.from(status), createdAt = createdAt,
)

fun ConversationDto.toDomain() = Conversation(id, peer.toDomain(), lastMessage?.toDomain(), unreadCount)
