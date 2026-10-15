package com.gorman.ourmemoryapp.domain.models

data class SubmissionAnalysisInput(
    val veteranName: String,
    val years: String,
    val birthDate: String,
    val deathDate: String,
    val biography: String,
    val rewards: List<String>,
    val cardMaterials: List<String>,
    val submissionText: String,
    val photoUrls: List<String>
)
