package com.example.echo.domain.model

/** Pure domain models. No annotations, no framework types — the layer everything else agrees on. */

data class Song(
    val id: Long,
    val title: String,
    val artistId: Long,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val genre: String?,
    val isLocal: Boolean,
    /** Populated by the repository from Room, never by the API. */
    val localFilePath: String? = null,
) {
    val isDownloaded: Boolean get() = localFilePath != null
    /** The player asks for this, and never has to know whether the bytes are local or remote. */
    val playbackUri: String get() = localFilePath ?: audioUrl
}

data class Artist(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val followers: Int,
)

data class Playlist(
    val id: Long,
    val title: String,
    val coverUrl: String?,
    val ownerId: Long?,
    val ownerName: String?,
    val kind: PlaylistKind,
    val songCount: Int,
    val songs: List<Song> = emptyList(),
)

enum class PlaylistKind { WORLD, LOCAL, USER;
    companion object {
        fun from(raw: String) = when (raw) {
            "world" -> WORLD
            "local" -> LOCAL
            else -> USER
        }
    }
}

data class User(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val isPremium: Boolean,
    val isFollowing: Boolean = false,
    val followers: Int = 0,
    val publicPlaylists: List<Playlist> = emptyList(),
)

data class Conversation(
    val id: Long,
    val peer: User,
    val lastMessage: Message?,
    val unreadCount: Int,
)

data class Message(
    val id: Long,
    val clientId: String?,
    val conversationId: Long,
    val senderId: Long,
    val body: String?,
    val song: Song?,
    val status: MessageStatus,
    val createdAt: Long,
)

/** SENDING exists only on-device: a message that hasn't been acked by the socket yet. */
enum class MessageStatus { SENDING, SENT, DELIVERED, READ;
    companion object {
        fun from(raw: String) = when (raw) {
            "DELIVERED" -> DELIVERED
            "READ" -> READ
            else -> SENT
        }
    }
}

data class SearchHistoryItem(val query: String, val searchedAt: Long)

enum class SearchFilter(val apiValue: String) { ALL("all"), SONG("song"), ARTIST("artist") }
