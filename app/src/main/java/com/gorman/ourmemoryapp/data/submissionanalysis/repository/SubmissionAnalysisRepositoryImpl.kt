package com.gorman.ourmemoryapp.data.submissionanalysis.repository

import com.google.gson.Gson
import com.gorman.ourmemoryapp.data.submissionanalysis.datasource.remote.ReviewPhotoLoader
import com.gorman.ourmemoryapp.data.submissionanalysis.datasource.remote.SubmissionAnalysisRemoteDataSource
import com.gorman.ourmemoryapp.data.submissionanalysis.mapper.toDomain
import com.gorman.ourmemoryapp.data.submissionanalysis.model.SubmissionAnalysisDto
import com.gorman.ourmemoryapp.domain.models.SubmissionAnalysis
import com.gorman.ourmemoryapp.domain.models.SubmissionAnalysisInput
import com.gorman.ourmemoryapp.domain.repository.SubmissionAnalysisRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class SubmissionAnalysisRepositoryImpl(
    private val remoteDataSource: SubmissionAnalysisRemoteDataSource,
    private val photoLoader: ReviewPhotoLoader,
    private val gson: Gson,
    private val ioDispatcher: CoroutineDispatcher
) : SubmissionAnalysisRepository {

    override suspend fun analyze(input: SubmissionAnalysisInput): SubmissionAnalysis = withContext(ioDispatcher) {
        val photos = photoLoader.load(input.photoUrls.take(MAX_PHOTOS))
        val json = remoteDataSource.analyze(prompt(input, photos.size), photos)
        gson.fromJson(json, SubmissionAnalysisDto::class.java).toDomain()
    }

    private fun prompt(input: SubmissionAnalysisInput, photoCount: Int): String {
        val materials = input.cardMaterials.joinToString(LINE_SEPARATOR) { MATERIAL_PREFIX + it }
        return """
            |КАРТОЧКА ВЕТЕРАНА
            |Имя: ${input.veteranName}
            |Годы жизни: ${input.years}
            |Дата рождения: ${input.birthDate}
            |Дата смерти: ${input.deathDate}
            |Награды: ${input.rewards.joinToString(LIST_SEPARATOR)}
            |Биография: ${input.biography}
            |Материалы карточки:
            |$materials
            |
            |ЗАЯВКА ПОСЕТИТЕЛЯ
            |${input.submissionText}
            |
            |Фотографий в заявке: $photoCount
        """.trimMargin()
    }

    companion object {
        private const val MAX_PHOTOS = 4
        private const val LIST_SEPARATOR = ", "
        private const val LINE_SEPARATOR = "\n"
        private const val MATERIAL_PREFIX = "- "
    }
}
