package com.gorman.ourmemoryapp.ui.admin.moderation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorman.ourmemoryapp.R
import com.gorman.ourmemoryapp.ui.admin.moderation.models.SubmissionReviewUiIntent
import com.gorman.ourmemoryapp.ui.admin.moderation.models.SubmissionReviewUiState
import com.gorman.ourmemoryapp.ui.admin.moderation.models.labelRes
import com.gorman.ourmemoryapp.ui.admin.moderation.viewmodels.SubmissionReviewViewModel
import com.gorman.ourmemoryapp.ui.common.ui.ErrorContent
import com.gorman.ourmemoryapp.ui.common.ui.LoadingContent
import com.gorman.ourmemoryapp.ui.common.ui.TopBarScaffold
import com.gorman.ourmemoryapp.ui.common.ui.TopBarSpacer
import com.gorman.ourmemoryapp.ui.common.ui.bottomBarContentPadding

@Composable
fun SubmissionReviewScreen(
    onBackClick: () -> Unit,
    submissionReviewViewModel: SubmissionReviewViewModel = hiltViewModel()
) {
    val state by submissionReviewViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if ((state as? SubmissionReviewUiState.Success)?.isFinished == true) onBackClick()
    }

    TopBarScaffold(title = stringResource(R.string.moderation), onBackClick = onBackClick) {
        when (val current = state) {
            SubmissionReviewUiState.Loading -> LoadingContent()
            SubmissionReviewUiState.Error -> ErrorContent()
            is SubmissionReviewUiState.Success -> SubmissionReviewContent(
                state = current,
                onUiIntent = submissionReviewViewModel::onUiIntent
            )
        }
    }
}

@Composable
private fun SubmissionReviewContent(
    state: SubmissionReviewUiState.Success,
    onUiIntent: (SubmissionReviewUiIntent) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp + bottomBarContentPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        item { TopBarSpacer() }
        item { SubmissionHeader(state = state) }
        item {
            OutlinedTextField(
                value = state.text,
                onValueChange = { onUiIntent(SubmissionReviewUiIntent.OnTextChange(it)) },
                label = { Text(text = stringResource(R.string.memories)) },
                enabled = state.isEditable,
                minLines = TEXT_MIN_LINES,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        item {
            OutlinedTextField(
                value = state.reply,
                onValueChange = { onUiIntent(SubmissionReviewUiIntent.OnReplyChange(it)) },
                label = { Text(text = stringResource(R.string.comment_for_author)) },
                enabled = state.isEditable,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        if (state.photos.isNotEmpty()) {
            item {
                ReviewPhotosRow(
                    photos = state.photos,
                    isEditable = state.isEditable,
                    onToggle = { onUiIntent(SubmissionReviewUiIntent.OnPhotoToggle(it)) }
                )
            }
        }
        item { SubmissionAnalysisSection(state = state, onUiIntent = onUiIntent) }
        state.failure?.let { failure ->
            item {
                Text(
                    text = stringResource(failure.messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        item { ReviewActions(state = state, onUiIntent = onUiIntent) }
    }
}

@Composable
private fun SubmissionHeader(state: SubmissionReviewUiState.Success) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = state.veteranName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "${state.date} · ${stringResource(state.status.labelRes)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = state.contact,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReviewActions(
    state: SubmissionReviewUiState.Success,
    onUiIntent: (SubmissionReviewUiIntent) -> Unit
) {
    val photoCaption = stringResource(R.string.from_family_archive)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        OutlinedButton(
            onClick = { onUiIntent(SubmissionReviewUiIntent.OnRejectClick) },
            enabled = state.isEditable,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(R.string.reject))
        }
        Button(
            onClick = { onUiIntent(SubmissionReviewUiIntent.OnApproveClick(photoCaption)) },
            enabled = state.isEditable,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(R.string.approve))
        }
    }
}

private const val TEXT_MIN_LINES = 5
