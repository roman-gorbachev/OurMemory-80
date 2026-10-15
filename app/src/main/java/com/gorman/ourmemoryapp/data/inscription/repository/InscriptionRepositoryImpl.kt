package com.gorman.ourmemoryapp.data.inscription.repository

import com.gorman.ourmemoryapp.data.inscription.datasource.remote.InscriptionRemoteDataSource
import com.gorman.ourmemoryapp.domain.repository.InscriptionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class InscriptionRepositoryImpl(
    private val remoteDataSource: InscriptionRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher
) : InscriptionRepository {

    override suspend fun recognizeInscription(jpeg: ByteArray): String = withContext(ioDispatcher) {
        remoteDataSource.recognize(jpeg)
    }
}
