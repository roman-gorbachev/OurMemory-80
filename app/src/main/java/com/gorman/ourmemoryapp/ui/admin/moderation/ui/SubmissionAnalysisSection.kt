package com.gorman.ourmemoryapp.ui.admin.moderation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gorman.ourmemoryapp.R
import com.gorman.ourmemoryapp.domain.models.SubmissionAnalysis
import com.gorman.ourmemoryapp.ui.admin.moderation.models.SubmissionReviewUiIntent
import com.gorman.ourmemoryapp.ui.admin.moderation.models.SubmissionReviewUiState
import com.gorman.ourmemoryapp.ui.details.models.Reward

@Composable
fun SubmissionAnalysisSection(
    state: SubmissionReviewUiState.Success,
    onUiIntent: (SubmissionReviewUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val rewardNames = Reward.entries.associate { it.id to stringResource(it.nameRes) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.ai_analysis),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            val analysis = state.analysis
            when {
                analysis != null -> AnalysisResult(
                    analysis = analysis,
                    isEditable = state.isEditable,
                    onUiIntent = onUiIntent
                )
                state.isAnalyzing -> AnalyzingRow()
                else -> {
                    Text(
                        text = stringResource(
                            if (state.analysisFailed) R.string.failed_to_analyze_msg else R.string.ai_analysis_hint_msg
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.analysisFailed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    OutlinedButton(
                        onClick = { onUiIntent(SubmissionReviewUiIntent.OnAnalyzeClick(rewardNames)) },
                        enabled = state.isEditable,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.analyze_with_ai))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyzingRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(R.string.analyzing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnalysisResult(
    analysis: SubmissionAnalysis,
    isEditable: Boolean,
    onUiIntent: (SubmissionReviewUiIntent) -> Unit
) {
    Text(text = analysis.summary, style = MaterialTheme.typography.bodyMedium)
    if (analysis.duplicateOf.isNotBlank()) {
        AnalysisBlock(title = stringResource(R.string.possible_duplicate_msg), lines = listOf(analysis.duplicateOf))
    }
    if (analysis.conflicts.isNotEmpty()) {
        AnalysisBlock(
            title = stringResource(R.string.possible_conflicts),
            lines = analysis.conflicts,
            isWarning = true
        )
    }
    if (analysis.documentTranscripts.isNotEmpty()) {
        AnalysisBlock(title = stringResource(R.string.document_transcripts), lines = analysis.documentTranscripts)
    }
    if (analysis.suggestedText.isNotBlank()) {
        AnalysisBlock(title = stringResource(R.string.suggested_text), lines = listOf(analysis.suggestedText))
        TextButton(
            onClick = { onUiIntent(SubmissionReviewUiIntent.OnUseSuggestedTextClick) },
            enabled = isEditable
        ) {
            Text(text = stringResource(R.string.use_suggested_text))
        }
    }
    if (analysis.suggestedReply.isNotBlank()) {
        AnalysisBlock(title = stringResource(R.string.suggested_reply), lines = listOf(analysis.suggestedReply))
        TextButton(
            onClick = { onUiIntent(SubmissionReviewUiIntent.OnUseSuggestedReplyClick) },
            enabled = isEditable
        ) {
            Text(text = stringResource(R.string.use_suggested_reply))
        }
    }
    Text(
        text = stringResource(R.string.ai_can_make_mistakes_msg),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun AnalysisBlock(
    title: String,
    lines: List<String>,
    isWarning: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                lines.forEach { line ->
                    Text(text = line, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
