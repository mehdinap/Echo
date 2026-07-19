package com.example.echo.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.data.local.prefs.TokenStore
import com.example.echo.domain.model.Message
import com.example.echo.domain.repository.ChatRepository
import com.example.echo.player.PlayerController
import com.example.echo.domain.repository.MusicRepository
import com.example.echo.core.util.Outcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val currentUserId: Long = -1,
    val peerIsTyping: Boolean = false,
    val draft: String = "",
)

sealed interface ChatEvent {
    data class DraftChanged(val text: String) : ChatEvent
    data object Send : ChatEvent
    data class ShareSong(val songId: Long) : ChatEvent
    data class PlaySong(val songId: Long) : ChatEvent
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController,
    tokenStore: TokenStore,
) : ViewModel() {

    private val conversationId: Long = checkNotNull(savedStateHandle["conversationId"])
    private val peerId: Long = checkNotNull(savedStateHandle["peerId"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val messages: Flow<PagingData<Message>> =
        chatRepository.pagedMessages(conversationId).cachedIn(viewModelScope)

    private var typingJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(currentUserId = tokenStore.currentUserId()) }
        }
        // Opening the screen marks everything as read; the peer sees two green ticks.
        viewModelScope.launch { chatRepository.markRead(conversationId) }

        viewModelScope.launch {
            chatRepository.typingEvents.collect { (userId, typing) ->
                if (userId == peerId) _uiState.update { it.copy(peerIsTyping = typing) }
            }
        }
        viewModelScope.launch {
            chatRepository.incomingMessages
                .filter { it.conversationId == conversationId }
                .collect { chatRepository.markRead(conversationId) }
        }
    }

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.DraftChanged -> {
                _uiState.update { it.copy(draft = event.text) }
                notifyTyping()
            }
            ChatEvent.Send -> {
                val body = _uiState.value.draft.trim()
                if (body.isEmpty()) return
                _uiState.update { it.copy(draft = "") }
                viewModelScope.launch { chatRepository.sendText(peerId, conversationId, body) }
            }
            is ChatEvent.ShareSong -> viewModelScope.launch {
                chatRepository.sendSong(peerId, conversationId, event.songId)
            }
            is ChatEvent.PlaySong -> viewModelScope.launch {
                (musicRepository.song(event.songId) as? Outcome.Success)?.let {
                    playerController.playSingle(it.data)
                }
            }
        }
    }

    /** "typing" turns on immediately and off after 1.5s of stillness. One event, not one per key. */
    private fun notifyTyping() {
        if (typingJob?.isActive != true) chatRepository.setTyping(peerId, true)
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(1_500)
            chatRepository.setTyping(peerId, false)
        }
    }

    override fun onCleared() {
        chatRepository.setTyping(peerId, false)
        super.onCleared()
    }
}
