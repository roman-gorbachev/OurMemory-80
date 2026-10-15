package com.gorman.ourmemoryapp.data.submissionanalysis.datasource.remote

import android.graphics.Bitmap

interface SubmissionAnalysisRemoteDataSource {
    suspend fun analyze(prompt: String, photos: List<Bitmap>): String
}
