package com.example.echo.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.core.util.Outcome
import com.example.echo.domain.model.User
import com.example.echo.domain.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isPurchasing: Boolean = false,
)

sealed interface ProfileEffect {
    data object Upgraded : ProfileEffect
    data object Failed : ProfileEffect
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val purchasing = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> =
        combine(authRepository.currentUser, purchasing) { user, busy -> ProfileUiState(user, busy) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    init { viewModelScope.launch { authRepository.refreshMe() } }

    /** Mock checkout: a short "processing" beat, then the flag flips server-side. */
    fun buyPremium() = viewModelScope.launch {
        purchasing.value = true
        delay(1_200)
        val result = authRepository.buyPremium()
        purchasing.value = false
        _effects.send(if (result is Outcome.Success) ProfileEffect.Upgraded else ProfileEffect.Failed)
    }

    fun changeAvatar(uri: Uri) = viewModelScope.launch {
        val encoded = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)?.let { bitmap ->
                    val size = maxOf(bitmap.width, bitmap.height)
                    val scaled = if (size > 512) {
                        val ratio = 512f / size
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * ratio).toInt(),
                            (bitmap.height * ratio).toInt(),
                            true,
                        )
                    } else bitmap

                    ByteArrayOutputStream().use { output ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 78, output)
                        if (scaled !== bitmap) scaled.recycle()
                        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    }
                    }
                }
            }.getOrNull()
        }
        val result = encoded?.let { authRepository.updateAvatarImage(it, "image/jpeg") }
        if (encoded == null || result is Outcome.Failure) {
            _effects.send(ProfileEffect.Failed)
        }
    }
}
