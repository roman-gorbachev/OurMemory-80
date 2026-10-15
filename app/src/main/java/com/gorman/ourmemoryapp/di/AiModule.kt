package com.gorman.ourmemoryapp.di

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.gorman.ourmemoryapp.data.inscription.datasource.remote.InscriptionRemoteDataSource
import com.gorman.ourmemoryapp.data.inscription.datasource.remote.InscriptionRemoteDataSourceImpl
import com.gorman.ourmemoryapp.data.inscription.repository.InscriptionRepositoryImpl
import com.gorman.ourmemoryapp.di.annotation.IoDispatcher
import com.gorman.ourmemoryapp.domain.repository.InscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    private const val MODEL_NAME = "gemini-3.8-flash"
    private const val TEMPERATURE = 0f

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel = Firebase.ai(
        backend = GenerativeBackend.agentPlatform(),
        useLimitedUseAppCheckTokens = true
    )
        .generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig { temperature = TEMPERATURE }
        )

    @Provides
    @Singleton
    fun provideInscriptionRemoteDataSource(model: GenerativeModel): InscriptionRemoteDataSource =
        InscriptionRemoteDataSourceImpl(model)

    @Provides
    @Singleton
    fun provideInscriptionRepository(
        remoteDataSource: InscriptionRemoteDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): InscriptionRepository = InscriptionRepositoryImpl(remoteDataSource, ioDispatcher)
}
