package com.example.echo.ui.social

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.core.util.Outcome
import com.example.echo.domain.model.User
import com.example.echo.domain.repository.ChatRepository
import com.example.echo.domain.repository.SocialRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class FindFriendsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _followOverrides = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val followOverrides: StateFlow<Map<Long, Boolean>> = _followOverrides.asStateFlow()

    /** Same debounce discipline as song search: the server sees one call, not one per keystroke. */
    val users: Flow<PagingData<User>> = _query
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { socialRepository.searchUsers(it.trim()) }
        .cachedIn(viewModelScope)

    fun onQueryChanged(value: String) { _query.value = value }

    fun toggleFollow(user: User) = viewModelScope.launch {
        val current = _followOverrides.value[user.id] ?: user.isFollowing
        val next = !current
        _followOverrides.update { it + (user.id to next) }
        val result = socialRepository.setFollowing(user.id, next)
        if (result is Outcome.Failure) {
            _followOverrides.update { it - user.id }
        }
    }
}

@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    init { refresh() }

    private fun refresh() = viewModelScope.launch {
        (socialRepository.following() as? Outcome.Success)?.let { _users.value = it.data }
    }

    fun unfollow(user: User) = viewModelScope.launch {
        socialRepository.setFollowing(user.id, false)
        _users.update { list -> list.filterNot { it.id == user.id } }
    }
}

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val socialRepository: SocialRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val userId: Long = checkNotNull(savedStateHandle["userId"])

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    /** Emits the conversation id once the server has one, so the screen can navigate to chat. */
    private val _openChat = Channel<Long>(Channel.BUFFERED)
    val openChat: Flow<Long> = _openChat.receiveAsFlow()

    init { refresh() }

    private fun refresh() = viewModelScope.launch {
        (socialRepository.user(userId) as? Outcome.Success)?.let { _user.value = it.data }
    }

    fun toggleFollow() = viewModelScope.launch {
        val current = _user.value ?: return@launch
        socialRepository.setFollowing(current.id, !current.isFollowing)
        _user.update { it?.copy(isFollowing = !current.isFollowing) }
    }

    fun message() = viewModelScope.launch {
        val result = chatRepository.openConversation(userId)
        if (result is Outcome.Success) _openChat.send(result.data)
    }
}
