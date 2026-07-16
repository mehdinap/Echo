package com.example.echo.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.echo.core.util.Outcome
import com.example.echo.core.util.runCatchingOutcome
import com.example.echo.data.local.dao.SearchHistoryDao
import com.example.echo.data.local.dao.SongDao
import com.example.echo.data.local.entity.SearchHistoryEntity
import com.example.echo.data.local.entity.toDomain
import com.example.echo.data.local.entity.toEntity
import com.example.echo.data.remote.api.EchoApi
import com.example.echo.data.remote.dto.toDomain
import com.example.echo.data.remote.paging.SearchPagingSource
import com.example.echo.data.remote.paging.SongPagingSource
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.*
import com.example.echo.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val api: EchoApi,
    private val songDao: SongDao,
    private val historyDao: SearchHistoryDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : MusicRepository {

    private val pagingConfig = PagingConfig(pageSize = 20, prefetchDistance = 5, enablePlaceholders = false)

    /** Songs arriving from the network are stamped with their local path, if we already have one. */
    private suspend fun localPath(id: Long) = songDao.localPath(id)

    override fun pagedSongs(sort: String): Flow<PagingData<Song>> =
        Pager(pagingConfig) { SongPagingSource(api, sort, ::localPath) }.flow

    override fun pagedSearch(query: String, filter: SearchFilter): Flow<PagingData<Song>> =
        Pager(pagingConfig) { SearchPagingSource(api, query, filter, ::localPath) }.flow

    override suspend fun carousel(): Outcome<List<Song>> = withContext(io) {
        runCatchingOutcome {
            api.carousel().map { it.toDomain(localPath(it.id)) }
                .also { songs -> songDao.upsertAll(songs.map { it.toEntity() }) }
        }
    }

    override suspend fun song(id: Long): Outcome<Song> = withContext(io) {
        runCatchingOutcome {
            // Cache first so a shared song opens instantly, even offline.
            songDao.byId(id)?.toDomain() ?: api.song(id).toDomain(localPath(id))
        }
    }

    override val likedSongs: Flow<List<Song>> =
        songDao.likedSongs().map { list -> list.map { it.toDomain() } }

    override suspend fun toggleLike(song: Song, liked: Boolean): Outcome<Unit> = withContext(io) {
        runCatchingOutcome {
            songDao.upsert(song.toEntity(liked = liked, path = localPath(song.id)))
            if (liked) api.like(song.id) else api.unlike(song.id)
        }
    }

    override suspend fun syncLikes(): Outcome<Unit> = withContext(io) {
        runCatchingOutcome {
            val remote = api.likes().map { it.toDomain(localPath(it.id)) }
            songDao.upsertAll(remote.map { it.toEntity(liked = true) })
        }
    }

    override suspend fun recentlyPlayed(): Outcome<List<Song>> = withContext(io) {
        runCatchingOutcome { api.recent().map { it.toDomain(localPath(it.id)) } }
    }

    override suspend fun markPlayed(songId: Long) {
        withContext(io) { runCatchingOutcome { api.markPlayed(songId) } }
    }

    override suspend fun playlists(kind: PlaylistKind?): Outcome<List<Playlist>> = withContext(io) {
        runCatchingOutcome {
            api.playlists(kind?.name?.lowercase()).map { it.toDomain() }
        }
    }

    override suspend fun myPlaylists(): Outcome<List<Playlist>> = withContext(io) {
        runCatchingOutcome { api.myPlaylists().map { it.toDomain() } }
    }

    override suspend fun playlist(id: Long): Outcome<Playlist> = withContext(io) {
        runCatchingOutcome {
            val dto = api.playlist(id)
            dto.toDomain().copy(songs = dto.songs.map { it.toDomain(localPath(it.id)) })
        }
    }

    override suspend fun topArtists(): Outcome<List<Artist>> = withContext(io) {
        runCatchingOutcome { api.topArtists().map { it.toDomain() } }
    }

    override val searchHistory: Flow<List<SearchHistoryItem>> =
        historyDao.history().map { list -> list.map { SearchHistoryItem(it.query, it.searchedAt) } }

    override suspend fun rememberSearch(query: String) = withContext(io) {
        if (query.isNotBlank()) historyDao.add(SearchHistoryEntity(query.trim(), System.currentTimeMillis()))
    }

    override suspend fun forgetSearch(query: String) = withContext(io) { historyDao.remove(query) }
    override suspend fun clearSearchHistory() = withContext(io) { historyDao.clear() }
}
