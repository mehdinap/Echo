package com.example.echo.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.example.echo.data.local.dao.SongDao
import com.example.echo.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Downloads one track to app-private storage and writes the path back into Room.
 * Retries with exponential backoff, survives the app being killed, and only runs
 * on an available network.
 */
@HiltWorker
class SongDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val songDao: SongDao,
    private val okHttpClient: OkHttpClient,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(io) {
        val songId = inputData.getLong(KEY_SONG_ID, -1L)
        val url = inputData.getString(KEY_AUDIO_URL) ?: return@withContext Result.failure()
        if (songId < 0) return@withContext Result.failure()

        val dir = File(applicationContext.filesDir, "downloads").apply { mkdirs() }
        val target = File(dir, "song_$songId.mp3")
        val partial = File(dir, "song_$songId.part")

        try {
            val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return@withContext Result.retry()

            val total = response.body?.contentLength() ?: -1L
            var written = 0L

            response.body?.byteStream()?.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        if (isStopped) { partial.delete(); return@withContext Result.failure() }
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) {
                            setProgress(workDataOf(KEY_PROGRESS to (written * 100 / total).toInt()))
                        }
                    }
                }
            } ?: return@withContext Result.retry()

            // Rename only after the full body landed: a half file is never treated as a download.
            if (!partial.renameTo(target)) return@withContext Result.retry()
            songDao.setLocalPath(songId, target.absolutePath, System.currentTimeMillis())
            Result.success(workDataOf(KEY_PROGRESS to 100))
        } catch (e: Exception) {
            partial.delete()
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_SONG_ID = "song_id"
        const val KEY_AUDIO_URL = "audio_url"
        const val KEY_PROGRESS = "progress"
        private const val MAX_ATTEMPTS = 3

        fun enqueue(context: Context, songId: Long, audioUrl: String) {
            val request = OneTimeWorkRequestBuilder<SongDownloadWorker>()
                .setInputData(workDataOf(KEY_SONG_ID to songId, KEY_AUDIO_URL to audioUrl))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
                .addTag(tagFor(songId))
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("download_$songId", ExistingWorkPolicy.KEEP, request)
        }

        fun tagFor(songId: Long) = "download_song_$songId"
    }
}
