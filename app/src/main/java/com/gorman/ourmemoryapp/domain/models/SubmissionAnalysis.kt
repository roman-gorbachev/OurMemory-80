package com.gorman.ourmemoryapp.domain.models

data class SubmissionAnalysis(
    val summary: String = "",
    val suggestedText: String = "",
    val conflicts: List<String> = emptyList(),
    val duplicateOf: String = "",
    val documentTranscripts: List<String> = emptyList(),
    val suggestedReply: String = ""
)
