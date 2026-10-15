package com.gorman.ourmemoryapp.ui.scan.models

import com.gorman.ourmemoryapp.domain.models.Veteran
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ScannerUiState(
    val candidates: ImmutableList<Veteran> = persistentListOf(),
    val status: InscriptionStatus = InscriptionStatus.IDLE
) {
    val isRecognizing = status == InscriptionStatus.RECOGNIZING
}
