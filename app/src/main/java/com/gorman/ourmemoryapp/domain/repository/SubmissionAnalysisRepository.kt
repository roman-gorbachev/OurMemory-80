package com.gorman.ourmemoryapp.domain.repository

import com.gorman.ourmemoryapp.domain.models.SubmissionAnalysis
import com.gorman.ourmemoryapp.domain.models.SubmissionAnalysisInput

interface SubmissionAnalysisRepository {
    suspend fun analyze(input: SubmissionAnalysisInput): SubmissionAnalysis
}
