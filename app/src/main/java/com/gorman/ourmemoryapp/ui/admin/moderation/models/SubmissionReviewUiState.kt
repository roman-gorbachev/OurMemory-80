package com.gorman.ourmemoryapp.ui.admin.moderation.models

import com.gorman.ourmemoryapp.domain.models.ModerationStatus
import com.gorman.ourmemoryapp.domain.models.SubmissionAnalysis
import kotlinx.collections.immutable.ImmutableList

sealed interface SubmissionReviewUiState {
    data object Loading : SubmissionReviewUiState
    data object Error : SubmissionReviewUiState
    data class Success(
        val veteranName: String,
        val contact: String,
        val date: String,
        val text: String,
        val reply: String,
        val photos: ImmutableList<ReviewPhotoUi>,
        val status: ModerationStatus,
        val isProcessing: Boolean,
        val failure: ReviewFailure?,
        val isFinished: Boolean,
        val analysis: SubmissionAnalysis?,
        val isAnalyzing: Boolean,
        val analysisFailed: Boolean
    ) : SubmissionReviewUiState {
        val isEditable = status == ModerationStatus.PENDING && !isProcessing
    }
}
