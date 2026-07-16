package com.example.echo.data.remote.socket

import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import com.example.echo.BuildConfig
import com.example.echo.data.local.prefs.TokenStore
import com.example.echo.data.remote.dto.MessageDto
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class TypingEvent(val userId: Long, val isTyping: Boolean)
data class StatusEvent(val conversationId: Long, val messageId: Long?, val status: String)
data class PresenceEvent(val userId: Long, val online: Boolean)

/**
 * Thin wrapper over socket.io. Everything it receives is republished as a Flow, so the rest
 * of the app never touches a callback. Polling is not used anywhere — the server pushes.
 */
@Singleton
class ChatSocket @Inject constructor(
    private val tokenStore: TokenStore,
    private val json: Json,
) {
    private var socket: Socket? = null

    private val _messages = MutableSharedFlow<MessageDto>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<MessageDto> = _messages

    private val _typing = MutableSharedFlow<TypingEvent>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val typing: SharedFlow<TypingEvent> = _typing

    private val _statuses = MutableSharedFlow<StatusEvent>(extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val statuses: SharedFlow<StatusEvent> = _statuses

    private val _presence = MutableSharedFlow<PresenceEvent>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val presence: SharedFlow<PresenceEvent> = _presence

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    suspend fun connect() {
        if (socket?.connected() == true) return
        val token = tokenStore.currentToken() ?: return

        val options = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .setReconnection(true)                 // socket.io handles backoff; we never poll
            .setReconnectionDelay(1_000)
            .setReconnectionDelayMax(10_000)
            .setTransports(arrayOf("websocket"))
            .build()

        socket = IO.socket(BuildConfig.BASE_URL, options).apply {
            on(Socket.EVENT_CONNECT) { _connected.tryEmit(true) }
            on(Socket.EVENT_DISCONNECT) { _connected.tryEmit(false) }

            on("message:new") { args ->
                args.firstOrNull()?.let { raw ->
                    runCatching { json.decodeFromString<MessageDto>(raw.toString()) }
                        .onSuccess { _messages.tryEmit(it) }
                }
            }
            on("typing") { args ->
                (args.firstOrNull() as? JSONObject)?.let {
                    _typing.tryEmit(TypingEvent(it.getLong("userId"), it.getBoolean("isTyping")))
                }
            }
            on("message:status") { args ->
                (args.firstOrNull() as? JSONObject)?.let {
                    _statuses.tryEmit(
                        StatusEvent(
                            conversationId = it.getLong("conversationId"),
                            messageId = if (it.has("messageId")) it.optLong("messageId") else null,
                            status = it.getString("status"),
                        )
                    )
                }
            }
            on("presence") { args ->
                (args.firstOrNull() as? JSONObject)?.let {
                    _presence.tryEmit(PresenceEvent(it.getLong("userId"), it.getBoolean("online")))
                }
            }
            connect()
        }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        _connected.value = false
    }

    /**
     * Sends a message and reports back the server-assigned id through [onAck], so the
     * optimistic row in Room can be promoted from SENDING to SENT.
     */
    fun sendMessage(
        peerId: Long,
        clientId: String,
        body: String? = null,
        songId: Long? = null,
        onAck: (MessageDto?) -> Unit,
    ) {
        val payload = JSONObject().apply {
            put("peerId", peerId)
            put("clientId", clientId)
            body?.let { put("body", it) }
            songId?.let { put("songId", it) }
        }
        socket?.emit("message:send", arrayOf(payload), Ack { args ->
            val response = args.firstOrNull() as? JSONObject
            val ok = response?.optBoolean("ok") == true
            val message = if (ok) {
                runCatching { json.decodeFromString<MessageDto>(response.getJSONObject("message").toString()) }.getOrNull()
            } else null
            onAck(message)
        })
    }

    fun markRead(conversationId: Long) {
        socket?.emit("message:read", JSONObject().put("conversationId", conversationId))
    }

    fun setTyping(peerId: Long, isTyping: Boolean) {
        socket?.emit("typing", JSONObject().put("peerId", peerId).put("isTyping", isTyping))
    }
}
