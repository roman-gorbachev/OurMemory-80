package com.gorman.ourmemoryapp.domain.repository

interface InscriptionRepository {
    suspend fun recognizeInscription(jpeg: ByteArray): String
}
