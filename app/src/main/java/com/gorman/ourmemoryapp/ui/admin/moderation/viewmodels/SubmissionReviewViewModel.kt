package com.gorman.ourmemoryapp.ui.admin.moderation.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorman.ourmemoryapp.domain.models.ModerationDeniedException
import com.gorman.ourmemoryapp.domain.models.Screen
import com.gorman.ourmemoryapp.domain.models.Submission
import com.gorman.ourmemoryapp.domain.models.SubmissionAnalysis
import com.gorman.ourmemoryapp.domain.models.SubmissionAnalysisInput
import com.gorman.ourmemoryapp.domain.models.SubmissionApproval
import com.gorman.ourmemoryapp.domain.models.Veteran
import com.gorman.ourmemoryapp.domain.models.VeteranInfoFormat
import com.gorman.ourmemoryapp.domain.repository.ModerationRepository
import com.gorman.ourmemoryapp.domain.repository.SubmissionAnalysisRepository
import com.gorman.ourmemoryapp.domain.repository.VeteransRepository
import com.gorman.ourmemoryapp.ui.admin.moderation.models.ReviewFailure
import com.gorman.ourmemoryapp.ui.admin.moderation.models.ReviewPhotoUi
import com.gorman.ourmemoryapp.ui.admin.moderation.models.SubmissionReviewUiIntent
import com.gorman.ourmemoryapp.ui.admin.moderation.models.SubmissionReviewUiState
import com.gorman.ourmemoryapp.ui.common.models.toDisplayDate
import com.gorman.ourmemoryapp.ui.details.models.parseRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubmissionReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val moderationRepository: ModerationRepository,
    private val veteransRepository: VeteransRepository,
    private val analysisRepository: SubmissionAnalysisRepository
) : ViewModel() {

    private val submissionId = savedStateHandle.get<String>(Screen.AdminSubmissionScreen.SUBMISSION_ID_ARG).orEmpty()
    private val form = MutableStateFlow(ReviewForm())

    private val loadedSubmission = flow { emit(runCatching { loadSubmission() }) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    val uiState = observeSubmissionReviewUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SubmissionReviewUiState.Loading
    )

    fun onUiIntent(intent: SubmissionReviewUiIntent) {
        when (intent) {
            is SubmissionReviewUiIntent.OnTextChange -> form.update { it.copy(editedText = intent.text) }
            is SubmissionReviewUiIntent.OnReplyChange -> form.update { it.copy(reply = intent.reply) }
            is SubmissionReviewUiIntent.OnPhotoToggle -> form.update { state ->
                val deselected = state.deselectedUrls
                state.copy(
                    deselectedUrls = if (intent.url in deselected) deselected - intent.url else deselected + intent.url
                )
            }
            is SubmissionReviewUiIntent.OnApproveClick -> approve(intent.photoCaption)
            SubmissionReviewUiIntent.OnRejectClick -> reject()
            is SubmissionReviewUiIntent.OnAnalyzeClick -> analyze(intent.rewardNames)
            SubmissionReviewUiIntent.OnUseSuggestedTextClick -> form.update { state ->
                state.copy(editedText = state.analysis?.suggestedText ?: state.editedText)
            }
            SubmissionReviewUiIntent.OnUseSuggestedReplyClick -> form.update { state ->
                state.copy(reply = state.analysis?.suggestedReply ?: state.reply)
            }
        }
    }

    private suspend fun loadSubmission(): LoadedSubmission {
        val submission = moderationRepository.observeSubmissions().first().first { it.id == submissionId }
        val veteranName = runCatching {
            veteransRepository.getAllVeterans().firstOrNull { it.id == submission.veteranId }?.name
        }.getOrNull()
        return LoadedSubmission(
            submission = submission,
            veteranName = veteranName ?: submission.veteranId,
            photoUrls = moderationRepository.resolvePhotoUrls(submission.photoPaths)
        )
    }

    private fun observeSubmissionReviewUiState() = combine(loadedSubmission, form) { result, form ->
        val loaded = result.getOrThrow()
        SubmissionReviewUiState.Success(
            veteranName = loaded.veteranName,
            contact = loaded.submission.contact,
            date = loaded.submission.createdAt.toDisplayDate(),
            text = form.editedText ?: loaded.submission.text,
            reply = form.reply ?: loaded.submission.reply,
            photos = loaded.photoUrls
                .map { ReviewPhotoUi(url = it, isSelected = it !in form.deselectedUrls) }
                .toPersistentList(),
            status = loaded.submission.status,
            isProcessing = form.isProcessing,
            failure = form.failure,
            isFinished = form.isFinished,
            analysis = form.analysis,
            isAnalyzing = form.isAnalyzing,
            analysisFailed = form.analysisFailed
        )
    }.catch<SubmissionReviewUiState> { error ->
        Log.e(LOG_TAG, "Failed to load submission $submissionId", error)
        emit(SubmissionReviewUiState.Error)
    }

    private fun approve(photoCaption: String) {
        val loaded = loadedSubmission.replayCache.firstOrNull()?.getOrNull() ?: return
        val state = form.value
        runDecision {
            moderationRepository.approve(
                SubmissionApproval(
                    submission = loaded.submission,
                    editedText = state.editedText ?: loaded.submission.text,
                    approvedPhotoUrls = loaded.photoUrls.filter { it !in state.deselectedUrls },
                    photoCaption = photoCaption,
                    reply = state.reply ?: loaded.submission.reply
                )
            )
        }
    }

    private fun analyze(rewardNames: Map<Int, String>) {
        val loaded = loadedSubmission.replayCache.firstOrNull()?.getOrNull() ?: return
        if (form.value.isAnalyzing) return
        form.update { it.copy(isAnalyzing = true, analysisFailed = false) }
        val state = form.value
        viewModelScope.launch {
            runCatching {
                val veteran = veteransRepository.getOriginalVeterans()
                    .firstOrNull { it.id == loaded.submission.veteranId } ?: Veteran()
                analysisRepository.analyze(
                    analysisInput(
                        veteran = veteran,
                        loaded = loaded,
                        text = state.editedText ?: loaded.submission.text,
                        photoUrls = loaded.photoUrls.filter { it !in state.deselectedUrls },
                        rewardNames = rewardNames
                    )
                )
            }.onSuccess { analysis ->
                form.update { it.copy(isAnalyzing = false, analysis = analysis) }
            }.onFailure { error ->
                Log.e(LOG_TAG, "Failed to analyze submission $submissionId", error)
                form.update { it.copy(isAnalyzing = false, analysisFailed = true) }
            }
        }
    }

    private fun analysisInput(
        veteran: Veteran,
        loaded: LoadedSubmission,
        text: String,
        photoUrls: List<String>,
        rewardNames: Map<Int, String>
    ) = SubmissionAnalysisInput(
        veteranName = veteran.name.ifBlank { loaded.veteranName },
        years = veteran.years,
        birthDate = veteran.birthDate,
        deathDate = veteran.deathDate,
        biography = veteran.allInfo.ifBlank { veteran.baseInfo },
        rewards = parseRewards(veteran.rewards).mapNotNull { rewardNames[it.reward.id] },
        cardMaterials = veteran.veteransInfo.filter { !it.contains(VeteranInfoFormat.LINK_MARKER) },
        submissionText = text,
        photoUrls = photoUrls
    )

    private fun reject() {
        val reply = form.value.reply ?: loadedSubmission.replayCache.firstOrNull()?.getOrNull()?.submission?.reply
        runDecision { moderationRepository.reject(submissionId, reply.orEmpty()) }
    }

    private fun runDecision(decision: suspend () -> Unit) {
        if (form.value.isProcessing) return
        form.update { it.copy(isProcessing = true, failure = null) }
        viewModelScope.launch {
            runCatching { decision() }
                .onSuccess { form.update { it.copy(isProcessing = false, isFinished = true) } }
                .onFailure { error ->
                    Log.e(LOG_TAG, "Failed to moderate submission $submissionId", error)
                    val failure = if (error is ModerationDeniedException) {
                        ReviewFailure.NO_PERMISSION
                    } else {
                        ReviewFailure.NETWORK
                    }
                    form.update { it.copy(isProcessing = false, failure = failure) }
                }
        }
    }

    private data class LoadedSubmission(
        val submission: Submission,
        val veteranName: String,
        val photoUrls: List<String>
    )

    private data class ReviewForm(
        val editedText: String? = null,
        val reply: String? = null,
        val deselectedUrls: Set<String> = emptySet(),
        val isProcessing: Boolean = false,
        val failure: ReviewFailure? = null,
        val isFinished: Boolean = false,
        val analysis: SubmissionAnalysis? = null,
        val isAnalyzing: Boolean = false,
        val analysisFailed: Boolean = false
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
        private const val LOG_TAG = "SubmissionReviewViewModel"
    }
}
