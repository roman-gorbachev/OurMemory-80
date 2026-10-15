package com.gorman.ourmemoryapp.data.submissionanalysis.model

import com.google.gson.annotations.SerializedName

data class SubmissionAnalysisDto(
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("suggested_text") val suggestedText: String? = null,
    @SerializedName("conflicts") val conflicts: List<String>? = null,
    @SerializedName("duplicate_of") val duplicateOf: String? = null,
    @SerializedName("document_transcripts") val documentTranscripts: List<String>? = null,
    @SerializedName("suggested_reply") val suggestedReply: String? = null
)
