package com.lastwave.app.di

import android.content.Context
import com.lastwave.app.data.MediaStoreScanner
import com.lastwave.app.data.OfflineMusicRepository
import com.lastwave.app.playback.OfflineAudioPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMediaStoreScanner(
        @ApplicationContext context: Context
    ): MediaStoreScanner = MediaStoreScanner(context)

    @Provides
    @Singleton
    fun provideOfflineMusicRepository(
        @ApplicationContext context: Context,
        scanner: MediaStoreScanner
    ): OfflineMusicRepository = OfflineMusicRepository(context, scanner)

    @Provides
    @Singleton
    fun provideOfflineAudioPlayer(
        @ApplicationContext context: Context
    ): OfflineAudioPlayer = OfflineAudioPlayer(context)
}
