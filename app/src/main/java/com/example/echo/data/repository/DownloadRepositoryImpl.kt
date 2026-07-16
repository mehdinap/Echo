package com.example.echo.data.repository

import android.content.Context
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.echo.data.local.dao.SongDao
import com.example.echo.data.local.entity.toDomain
import com.example.echo.data.local.entity.toEntity
import com.example.echo.data.local.prefs.SettingsStore
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.Song
import com.example.echo.domain.repository.DownloadRepository
import com.example.echo.domain.repository.DownloadSort
import com.example.echo.download.SongDownloadWorker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val settingsStore: SettingsStore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DownloadRepository {

    override fun downloads(sort: DownloadSort): Flow<List<Song>> = when (sort) {
        DownloadSort.DATE -> songDao.downloadsByDate()
        DownloadSort.TITLE -> songDao.downloadsByTitle()
        DownloadSort.ARTIST -> songDao.downloadsByArtist()
    }.map { list -> list.map { it.toDomain() } }

    /** The Premium gate lives here, not in the UI — one rule, one place. */
    override suspend fun enqueueDownload(song: Song): Boolean = withContext(io) {
        if (!settingsStore.current().isPremium) return@withContext false
        songDao.upsert(song.toEntity())          // make sure metadata exists before the file lands
        SongDownloadWorker.enqueue(context, song.id, song.audioUrl)
        true
    }

    override suspend fun deleteDownload(songId: Long): Unit = withContext(io) {
        WorkManager.getInstance(context).cancelAllWorkByTag(SongDownloadWorker.tagFor(songId))
        songDao.localPath(songId)?.let { File(it).delete() }
        songDao.setLocalPath(songId, null, null)
    }

    override suspend fun localPathOf(songId: Long): String? = withContext(io) { songDao.localPath(songId) }
}
