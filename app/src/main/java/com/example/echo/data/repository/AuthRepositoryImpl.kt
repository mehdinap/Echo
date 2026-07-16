package com.example.echo.data.repository

import com.example.echo.core.util.Outcome
import com.example.echo.core.util.runCatchingOutcome
import com.example.echo.data.local.prefs.SettingsStore
import com.example.echo.data.local.prefs.TokenStore
import com.example.echo.data.remote.api.EchoApi
import com.example.echo.data.remote.dto.AuthRequest
import com.example.echo.data.remote.dto.ProfileUpdateDto
import com.example.echo.data.remote.dto.toDomain
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.User
import com.example.echo.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: EchoApi,
    private val tokenStore: TokenStore,
    private val settingsStore: SettingsStore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AuthRepository {

    private val userState = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = userState.asStateFlow()
    override val isLoggedIn: Flow<Boolean> = tokenStore.token.map { !it.isNullOrBlank() }

    override suspend fun login(username: String, password: String) = withContext(io) {
        runCatchingOutcome {
            val res = api.login(AuthRequest(username, password))
            tokenStore.save(res.token, res.user.id)
            settingsStore.setPremium(res.user.isPremium)
            res.user.toDomain().also { userState.value = it }
        }
    }

    override suspend fun register(username: String, password: String, displayName: String) = withContext(io) {
        runCatchingOutcome {
            val res = api.register(AuthRequest(username, password, displayName))
            res.user.toDomain()
        }
    }

    override suspend fun refreshMe(): Outcome<User> = withContext(io) {
        runCatchingOutcome {
            api.me().toDomain().also {
                userState.value = it
                settingsStore.setPremium(it.isPremium)
            }
        }
    }

    /** Mock purchase: the server flips the flag, DataStore mirrors it for offline reads. */
    override suspend fun buyPremium(): Outcome<User> = withContext(io) {
        runCatchingOutcome {
            api.buyPremium().toDomain().also {
                userState.value = it
                settingsStore.setPremium(true)
            }
        }
    }

    override suspend fun updateEchotar(url: String): Outcome<User> = withContext(io) {
        runCatchingOutcome {
            api.updateProfile(ProfileUpdateDto(avatarUrl = url)).toDomain().also { userState.value = it }
        }
    }

    override suspend fun updateAvatarImage(base64: String, mimeType: String): Outcome<User> = withContext(io) {
        runCatchingOutcome {
            api.updateProfile(
                ProfileUpdateDto(avatarData = base64, avatarMimeType = mimeType)
            ).toDomain().also { userState.value = it }
        }
    }

    override suspend fun logout() = withContext(io) {
        tokenStore.clear()
        userState.value = null
    }
}
