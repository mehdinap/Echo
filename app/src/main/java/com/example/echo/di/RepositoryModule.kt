package com.example.echo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.echo.data.repository.*
import com.example.echo.domain.repository.*
import javax.inject.Singleton

/** Interfaces come from `domain`, implementations from `data`. The UI only ever sees the interface. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun authRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun musicRepository(impl: MusicRepositoryImpl): MusicRepository
    @Binds @Singleton abstract fun downloadRepository(impl: DownloadRepositoryImpl): DownloadRepository
    @Binds @Singleton abstract fun socialRepository(impl: SocialRepositoryImpl): SocialRepository
    @Binds @Singleton abstract fun chatRepository(impl: ChatRepositoryImpl): ChatRepository
}
