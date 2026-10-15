package com.gorman.ourmemoryapp.ui.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorman.ourmemoryapp.R
import com.gorman.ourmemoryapp.ui.common.ui.CategoryFilterChips
import com.gorman.ourmemoryapp.ui.common.ui.EmptyContent
import com.gorman.ourmemoryapp.ui.common.ui.ErrorContent
import com.gorman.ourmemoryapp.ui.common.ui.VeteranItem
import com.gorman.ourmemoryapp.ui.common.ui.bottomBarContentPadding
import com.gorman.ourmemoryapp.ui.common.ui.statusBarContentPadding
import com.gorman.ourmemoryapp.ui.home.models.HomeUiIntent
import com.gorman.ourmemoryapp.ui.home.models.HomeUiState
import com.gorman.ourmemoryapp.ui.home.viewmodels.HomeViewModel

@Composable
fun MainScreen(
    onItemClick: (String) -> Unit,
    onScanClick: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val onUiIntent = homeViewModel::onUiIntent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is HomeUiState.Error -> ErrorContent(modifier = Modifier.statusBarsPadding())
            HomeUiState.Loading -> HomeLoadingContent()
            is HomeUiState.Success -> OurMemoryScreen(
                state = state,
                onItemClick = onItemClick,
                onScanClick = onScanClick,
                onUiIntent = onUiIntent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OurMemoryScreen(
    state: HomeUiState.Success,
    onItemClick: (String) -> Unit,
    onScanClick: () -> Unit,
    onUiIntent: (HomeUiIntent) -> Unit
) {
    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onUiIntent(HomeUiIntent.OnRefresh) },
        state = refreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = state.isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarContentPadding(),
                bottom = 16.dp + bottomBarContentPadding()
            )
        ) {
            item {
                HomeHeader(
                    state = state,
                    onScanClick = onScanClick,
                    onUiIntent = onUiIntent
                )
            }
            if (state.veterans.isEmpty()) {
                item {
                    if (state.checkedWar || state.checkedArt) {
                        EmptyContent(
                            iconRes = R.drawable.search_icon,
                            message = stringResource(R.string.nothing_found_msg, state.search)
                        )
                    } else {
                        EmptyContent(
                            iconRes = R.drawable.star,
                            message = stringResource(R.string.chooseCategory)
                        )
                    }
                }
            }
            if (state.veterans.isNotEmpty() && state.anniversaries.isNotEmpty()) {
                item {
                    AnniversariesRow(
                        anniversaries = state.anniversaries,
                        onVeteranClick = onItemClick,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }
            items(state.veterans, key = { it.id }) { veteran ->
                VeteranItem(
                    veteran = veteran,
                    onClick = { onItemClick(veteran.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState.Success,
    onScanClick: () -> Unit,
    onUiIntent: (HomeUiIntent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
            )
            IconButton(onClick = onScanClick, modifier = Modifier.padding(end = 8.dp)) {
                Icon(
                    painter = painterResource(R.drawable.qr_code_scanner),
                    contentDescription = stringResource(R.string.scan_qr_code_or_name),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        SearchField(
            search = state.search,
            onSearchTextChange = { onUiIntent(HomeUiIntent.OnSearchChange(it)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        CategoryFilterChips(
            checkedWar = state.checkedWar,
            checkedArt = state.checkedArt,
            onCheckedWarChange = { onUiIntent(HomeUiIntent.OnCheckedWarChange(it)) },
            onCheckedArtChange = { onUiIntent(HomeUiIntent.OnCheckedArtChange(it)) },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SearchField(
    search: String,
    onSearchTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = search,
        onValueChange = onSearchTextChange,
        placeholder = { Text(text = stringResource(R.string.search)) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.search_icon),
                contentDescription = null
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    )
}
