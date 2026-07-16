package com.example.echo.data.remote.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.echo.data.remote.api.EchoApi
import com.example.echo.data.remote.dto.toDomain
import com.example.echo.domain.model.SearchFilter
import com.example.echo.domain.model.Song
import com.example.echo.domain.model.User
import retrofit2.HttpException
import java.io.IOException

private const val PAGE_SIZE = 20

/** Songs list (popular / newest). Never materialises the whole catalog in memory. */
class SongPagingSource(
    private val api: EchoApi,
    private val sort: String,
    private val localPathOf: suspend (Long) -> String?,
) : PagingSource<Int, Song>() {

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? =
        state.anchorPosition?.let { state.closestPageToPosition(it)?.prevKey?.plus(1) }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> = try {
        val page = params.key ?: 0
        val dto = api.songs(page, params.loadSize.coerceAtMost(PAGE_SIZE), sort)
        val songs = dto.items.map { it.toDomain(localPathOf(it.id)) }
        LoadResult.Page(
            data = songs,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (dto.hasNext) page + 1 else null,
        )
    } catch (e: IOException) {
        LoadResult.Error(e)
    } catch (e: HttpException) {
        LoadResult.Error(e)
    }
}

/** Search results. A new source is created every time the query or filter changes. */
class SearchPagingSource(
    private val api: EchoApi,
    private val query: String,
    private val filter: SearchFilter,
    private val localPathOf: suspend (Long) -> String?,
) : PagingSource<Int, Song>() {

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> = try {
        val page = params.key ?: 0
        val dto = api.search(query, filter.apiValue, page, params.loadSize.coerceAtMost(PAGE_SIZE))
        LoadResult.Page(
            data = dto.items.map { it.toDomain(localPathOf(it.id)) },
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (dto.hasNext) page + 1 else null,
        )
    } catch (e: IOException) {
        LoadResult.Error(e)
    } catch (e: HttpException) {
        LoadResult.Error(e)
    }
}

/** People search, used by "find friends". */
class UserPagingSource(
    private val api: EchoApi,
    private val query: String,
) : PagingSource<Int, User>() {

    override fun getRefreshKey(state: PagingState<Int, User>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, User> = try {
        val page = params.key ?: 0
        val dto = api.searchUsers(query, page, params.loadSize.coerceAtMost(PAGE_SIZE))
        LoadResult.Page(
            data = dto.items.map { it.toDomain() },
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (dto.hasNext) page + 1 else null,
        )
    } catch (e: IOException) {
        LoadResult.Error(e)
    } catch (e: HttpException) {
        LoadResult.Error(e)
    }
}
