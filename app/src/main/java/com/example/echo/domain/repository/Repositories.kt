package com.example.echo.domain.repository

import androidx.paging.PagingData
import com.example.echo.core.util.Outcome
import com.example.echo.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val isLoggedIn: Flow<Boolean>
    suspend fun login(username: String, password: String): Outcome<User>
    suspend fun register(username: String, password: String, displayName: String): Outcome<User>
    suspend fun refreshMe(): Outcome<User>
    suspend fun buyPremium(): Outcome<User>
    suspend fun updateEchotar(url: String): Outcome<User>
    suspend fun updateAvatarImage(base64: String, mimeType: String): Outcome<User>
    suspend fun logout()
}

interface MusicRepository {
    fun pagedSongs(sort: String): Flow<PagingData<Song>>
    fun pagedSearch(query: String, filter: SearchFilter): Flow<PagingData<Song>>
    suspend fun carousel(): Outcome<List<Song>>
    suspend fun song(id: Long): Outcome<Song>

    val likedSongs: Flow<List<Song>>
    suspend fun toggleLike(song: Song, liked: Boolean): Outcome<Unit>
    suspend fun syncLikes(): Outcome<Unit>

    suspend fun recentlyPlayed(): Outcome<List<Song>>
    suspend fun markPlayed(songId: Long)

    suspend fun playlists(kind: PlaylistKind?): Outcome<List<Playlist>>
    suspend fun myPlaylists(): Outcome<List<Playlist>>
    suspend fun playlist(id: Long): Outcome<Playlist>

    suspend fun topArtists(): Outcome<List<Artist>>

    // ---- search history (Room) ----
    val searchHistory: Flow<List<SearchHistoryItem>>
    suspend fun rememberSearch(query: String)
    suspend fun forgetSearch(query: String)
    suspend fun clearSearchHistory()
}

interface DownloadRepository {
    fun downloads(sort: DownloadSort): Flow<List<Song>>
    /** Enqueues a WorkManager job. Returns false when the user isn't Premium. */
    suspend fun enqueueDownload(song: Song): Boolean
    suspend fun deleteDownload(songId: Long)
    suspend fun localPathOf(songId: Long): String?
}

enum class DownloadSort { DATE, TITLE, ARTIST }

interface SocialRepository {
    fun searchUsers(query: String): Flow<PagingData<User>>
    suspend fun user(id: Long): Outcome<User>
    suspend fun setFollowing(userId: Long, follow: Boolean): Outcome<Unit>
    suspend fun following(): Outcome<List<User>>
}

interface ChatRepository {
    suspend fun conversations(): Outcome<List<Conversation>>
    suspend fun openConversation(peerId: Long): Outcome<Long>
    fun pagedMessages(conversationId: Long): Flow<PagingData<Message>>

    /** Hot streams driven by the WebSocket. */
    val incomingMessages: Flow<Message>
    val typingEvents: Flow<Pair<Long, Boolean>>

    suspend fun connect()
    suspend fun disconnect()
    suspend fun sendText(peerId: Long, conversationId: Long, body: String)
    suspend fun sendSong(peerId: Long, conversationId: Long, songId: Long)
    suspend fun markRead(conversationId: Long)
    fun setTyping(peerId: Long, typing: Boolean)
}
