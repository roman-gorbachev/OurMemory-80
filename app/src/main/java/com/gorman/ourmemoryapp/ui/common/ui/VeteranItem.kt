package com.gorman.ourmemoryapp.ui.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gorman.ourmemoryapp.R
import com.gorman.ourmemoryapp.domain.models.Veteran

@Composable
fun VeteranItem(
    veteran: Veteran,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseInfoMaxLines: Int = BASE_INFO_MAX_LINES
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = veteran.portrait,
            contentDescription = veteran.name,
            placeholder = painterResource(R.drawable.portrait_placeholder),
            error = painterResource(R.drawable.portrait_placeholder),
            fallback = painterResource(R.drawable.portrait_placeholder),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .weight(PORTRAIT_WEIGHT)
                .aspectRatio(PORTRAIT_ASPECT_RATIO)
                .clip(RoundedCornerShape(10.dp))
        )
        Column(modifier = Modifier.weight(TEXT_WEIGHT)) {
            Text(
                text = veteran.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (veteran.years.isNotBlank()) {
                Text(
                    text = veteran.years,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = veteran.baseInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = baseInfoMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private const val PORTRAIT_WEIGHT = 1f
private const val TEXT_WEIGHT = 2f
private const val PORTRAIT_ASPECT_RATIO = 0.8f
private const val BASE_INFO_MAX_LINES = 5
