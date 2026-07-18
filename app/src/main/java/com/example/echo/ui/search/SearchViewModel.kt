package com.example.echo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.domain.model.SearchFilter
import com.example.echo.domain.model.SearchHistoryItem
import com.example.echo.domain.model.Song
import com.example.echo.domain.repository.MusicRepository
import com.example.echo.player.PlayerController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val history: List<SearchHistoryItem> = emptyList(),
)

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data class FilterChanged(val filter: SearchFilter) : SearchEvent
    data class HistoryClicked(val query: String) : SearchEvent
    data class HistoryRemoved(val query: String) : SearchEvent
    data object HistoryCleared : SearchEvent
    data class Submitted(val query: String) : SearchEvent
    data class PlaySong(val song: Song) : SearchEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(SearchFilter.ALL)

    val uiState: StateFlow<SearchUiState> = combine(
        query, filter, musicRepository.searchHistory,
    ) { q, f, history -> SearchUiState(q, f, history) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    /**
     * Typing does not hit the server. `debounce` waits 350ms of silence, `distinctUntilChanged`
     * drops repeats, and `flatMapLatest` cancels the previous Pager when a newer query arrives.
     */
    val results: Flow<PagingData<Song>> =
        combine(
            query.debounce(350).distinctUntilChanged(),
            filter,
        ) { q, f -> q.trim() to f }
            .flatMapLatest { (q, f) ->
                if (q.length < 2) flowOf(PagingData.empty())
                else musicRepository.pagedSearch(q, f)
            }
            .cachedIn(viewModelScope)

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> query.value = event.query
            is SearchEvent.FilterChanged -> filter.value = event.filter
            is SearchEvent.HistoryClicked -> query.value = event.query
            is SearchEvent.HistoryRemoved -> viewModelScope.launch { musicRepository.forgetSearch(event.query) }
            SearchEvent.HistoryCleared -> viewModelScope.launch { musicRepository.clearSearchHistory() }
            is SearchEvent.Submitted -> viewModelScope.launch { musicRepository.rememberSearch(event.query) }
            is SearchEvent.PlaySong -> playerController.playSingle(event.song)
        }
    }
}
