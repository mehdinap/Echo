package com.example.echo.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.core.designsystem.component.EmptyState
import com.example.echo.core.designsystem.component.pressScale
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.core.util.Outcome
import com.example.echo.domain.model.Conversation
import com.example.echo.domain.repository.ChatRepository
import com.example.echo.ui.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.echo.R
import kotlinx.coroutines.flow.update

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations = _conversations.asStateFlow()

    init {
        refresh()
        // Any inbound message reshuffles the list, so watch the socket rather than re-polling.
        viewModelScope.launch { chatRepository.incomingMessages.collect { refresh() } }
    }

    fun refresh() = viewModelScope.launch {
        (chatRepository.conversations() as? Outcome.Success)?.let { _conversations.value = it.data }
    }

    fun markAsRead(conversationId: Long) = viewModelScope.launch {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) conv.copy(unreadCount = 0) else conv
            }
        }
        chatRepository.markRead(conversationId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    navController: NavController,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Destination.FindFriends.route) }) {
                        Icon(Icons.Filled.PersonSearch, stringResource(R.string.find_friends))
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_playlist_title),
                body = stringResource(R.string.empty_playlist_body),
                actionLabel = stringResource(R.string.find_friends),
                onAction = { navController.navigate(Destination.FindFriends.route) },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(Modifier.padding(padding)) {
            items(conversations, key = { it.id }) { conversation ->
                ListItem(
                    headlineContent = { Text(conversation.peer.displayName) },
                    supportingContent = {
                        Text(
                            conversation.lastMessage?.body
                                ?: stringResource(R.string.chat_shared_a_song),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        AsyncImage(
                            model = conversation.peer.avatarUrl,
                            placeholder = painterResource(R.drawable.ic_default_avatar),
                            error = painterResource(R.drawable.ic_default_avatar),
                            fallback = painterResource(R.drawable.ic_default_avatar),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(EchoTheme.sizes.avatar)
                                .clip(CircleShape),
                        )
                    },
                    trailingContent = {
                        if (conversation.unreadCount > 0) {
                            Badge { Text(conversation.unreadCount.toString()) }
                        }
                    },
                    modifier = Modifier.pressScale(pressedScale = 0.99f) {
                        viewModel.markAsRead(conversation.id)
                        navController.navigate(Destination.Chat.of(conversation.id, conversation.peer.id))
                    },
                )
            }
        }
    }
}
