package com.gorman.ourmemoryapp.ui.admin.moderation.models

sealed interface SubmissionReviewUiIntent {
    data class OnTextChange(val text: String) : SubmissionReviewUiIntent
    data class OnReplyChange(val reply: String) : SubmissionReviewUiIntent
    data class OnPhotoToggle(val url: String) : SubmissionReviewUiIntent
    data class OnApproveClick(val photoCaption: String) : SubmissionReviewUiIntent
    data object OnRejectClick : SubmissionReviewUiIntent
    data class OnAnalyzeClick(val rewardNames: Map<Int, String>) : SubmissionReviewUiIntent
    data object OnUseSuggestedTextClick : SubmissionReviewUiIntent
    data object OnUseSuggestedReplyClick : SubmissionReviewUiIntent
}
