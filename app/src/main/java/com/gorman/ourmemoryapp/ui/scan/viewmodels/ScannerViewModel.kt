package com.gorman.ourmemoryapp.ui.scan.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorman.ourmemoryapp.domain.logic.VeteranNameMatcher
import com.gorman.ourmemoryapp.domain.repository.InscriptionRepository
import com.gorman.ourmemoryapp.domain.repository.VeteransRepository
import com.gorman.ourmemoryapp.ui.scan.models.InscriptionStatus
import com.gorman.ourmemoryapp.ui.scan.models.ScannerUiIntent
import com.gorman.ourmemoryapp.ui.scan.models.ScannerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val veteransRepository: VeteransRepository,
    private val inscriptionRepository: InscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState = _uiState.asStateFlow()

    fun onUiIntent(intent: ScannerUiIntent) {
        when (intent) {
            is ScannerUiIntent.OnPhotoTaken -> recognize(intent.jpeg)
        }
    }

    private fun recognize(jpeg: ByteArray) {
        if (_uiState.value.isRecognizing) return
        _uiState.update { ScannerUiState(status = InscriptionStatus.RECOGNIZING) }
        viewModelScope.launch {
            runCatching {
                val inscription = inscriptionRepository.recognizeInscription(jpeg)
                val localizedById = veteransRepository.getAllVeterans().associateBy { it.id }
                VeteranNameMatcher.candidates(inscription, veteransRepository.getOriginalVeterans())
                    .map { localizedById[it.id] ?: it }
            }.onSuccess { candidates ->
                _uiState.update {
                    ScannerUiState(
                        candidates = candidates.toPersistentList(),
                        status = if (candidates.isEmpty()) InscriptionStatus.NOT_FOUND else InscriptionStatus.FOUND
                    )
                }
            }.onFailure { error ->
                Log.e(LOG_TAG, "Failed to recognize the inscription", error)
                _uiState.update { ScannerUiState(candidates = persistentListOf(), status = InscriptionStatus.FAILED) }
            }
        }
    }

    companion object {
        private const val LOG_TAG = "ScannerViewModel"
    }
}
