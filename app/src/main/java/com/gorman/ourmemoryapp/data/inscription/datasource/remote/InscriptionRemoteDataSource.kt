package com.gorman.ourmemoryapp.data.inscription.datasource.remote

interface InscriptionRemoteDataSource {
    suspend fun recognize(jpeg: ByteArray): String
}
