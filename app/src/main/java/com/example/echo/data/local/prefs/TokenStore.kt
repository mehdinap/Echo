package com.example.echo.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authStore by preferencesDataStore(name = "ava_auth")

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = longPreferencesKey("user_id")
    }

    val token: Flow<String?> = context.authStore.data.map { it[Keys.TOKEN] }
    val userId: Flow<Long?> = context.authStore.data.map { it[Keys.USER_ID] }

    suspend fun currentToken(): String? = token.first()
    suspend fun currentUserId(): Long = userId.first() ?: -1L

    suspend fun save(token: String, userId: Long) = context.authStore.edit {
        it[Keys.TOKEN] = token
        it[Keys.USER_ID] = userId
    }

    suspend fun clear() = context.authStore.edit { it.clear() }
}
