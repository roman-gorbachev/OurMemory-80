package com.gorman.ourmemoryapp.data.submissionanalysis.mapper

import com.gorman.ourmemoryapp.data.submissionanalysis.model.SubmissionAnalysisDto
import com.gorman.ourmemoryapp.domain.models.SubmissionAnalysis

fun SubmissionAnalysisDto.toDomain() = SubmissionAnalysis(
    summary = summary.orEmpty(),
    suggestedText = suggestedText.orEmpty(),
    conflicts = conflicts.orEmpty().filter { it.isNotBlank() },
    duplicateOf = duplicateOf.orEmpty(),
    documentTranscripts = documentTranscripts.orEmpty().filter { it.isNotBlank() },
    suggestedReply = suggestedReply.orEmpty()
)
