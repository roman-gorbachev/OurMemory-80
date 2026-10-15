package com.gorman.ourmemoryapp.di

import com.google.firebase.Firebase
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.gson.Gson
import com.gorman.ourmemoryapp.data.inscription.datasource.remote.InscriptionRemoteDataSource
import com.gorman.ourmemoryapp.data.inscription.datasource.remote.InscriptionRemoteDataSourceImpl
import com.gorman.ourmemoryapp.data.inscription.repository.InscriptionRepositoryImpl
import com.gorman.ourmemoryapp.data.submissionanalysis.datasource.remote.ReviewPhotoLoader
import com.gorman.ourmemoryapp.data.submissionanalysis.datasource.remote.SubmissionAnalysisRemoteDataSource
import com.gorman.ourmemoryapp.data.submissionanalysis.datasource.remote.SubmissionAnalysisRemoteDataSourceImpl
import com.gorman.ourmemoryapp.data.submissionanalysis.repository.SubmissionAnalysisRepositoryImpl
import com.gorman.ourmemoryapp.di.annotation.IoDispatcher
import com.gorman.ourmemoryapp.domain.repository.InscriptionRepository
import com.gorman.ourmemoryapp.domain.repository.SubmissionAnalysisRepository
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

    @Provides
    @Singleton
    fun provideFirebaseAi(): FirebaseAI = Firebase.ai(
        backend = GenerativeBackend.agentPlatform(),
        useLimitedUseAppCheckTokens = true
    )

    @Provides
    @Singleton
    fun provideInscriptionRemoteDataSource(firebaseAi: FirebaseAI): InscriptionRemoteDataSource =
        InscriptionRemoteDataSourceImpl(firebaseAi, MODEL_NAME)

    @Provides
    @Singleton
    fun provideInscriptionRepository(
        remoteDataSource: InscriptionRemoteDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): InscriptionRepository = InscriptionRepositoryImpl(remoteDataSource, ioDispatcher)

    @Provides
    @Singleton
    fun provideSubmissionAnalysisRemoteDataSource(firebaseAi: FirebaseAI): SubmissionAnalysisRemoteDataSource =
        SubmissionAnalysisRemoteDataSourceImpl(firebaseAi, MODEL_NAME)

    @Provides
    @Singleton
    fun provideSubmissionAnalysisRepository(
        remoteDataSource: SubmissionAnalysisRemoteDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): SubmissionAnalysisRepository = SubmissionAnalysisRepositoryImpl(
        remoteDataSource = remoteDataSource,
        photoLoader = ReviewPhotoLoader(),
        gson = Gson(),
        ioDispatcher = ioDispatcher
    )
}
