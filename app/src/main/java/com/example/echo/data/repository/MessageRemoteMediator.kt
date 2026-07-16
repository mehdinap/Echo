package com.example.echo.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.echo.data.local.dao.ChatDao
import com.example.echo.data.local.dao.SongDao
import com.example.echo.data.local.entity.MessageEntity
import com.example.echo.data.local.entity.toEntity
import com.example.echo.data.remote.api.EchoApi
import com.example.echo.data.remote.dto.toDomain
import java.io.IOException

/**
 * Fills Room with older pages of history when the user scrolls back far enough.
 * PREPEND is skipped: new messages arrive over the socket, not by paging forward.
 */
@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val conversationId: Long,
    private val api: EchoApi,
    private val chatDao: ChatDao,
    private val songDao: SongDao,
) : RemoteMediator<Int, MessageEntity>() {

    private var page = 0

    override suspend fun initialize() = InitializeAction.SKIP_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageEntity>,
    ): MediatorResult {
        return try {
            page = when (loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> page + 1
            }

            val response = api.messages(conversationId, page, state.config.pageSize)
            response.items.forEach { dto ->
                dto.song?.let { songDao.upsert(it.toDomain().toEntity()) }
            }
            chatDao.upsertAll(response.items.map { it.toDomain().toEntity() })
            MediatorResult.Success(endOfPaginationReached = !response.hasNext)
        } catch (e: IOException) {
            // Offline: whatever Room already holds is still shown.
            MediatorResult.Error(e)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
