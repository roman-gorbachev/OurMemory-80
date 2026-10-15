package com.gorman.ourmemoryapp.ui.more.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorman.ourmemoryapp.R
import com.gorman.ourmemoryapp.domain.models.AppSettings
import com.gorman.ourmemoryapp.domain.models.TextScale
import com.gorman.ourmemoryapp.domain.models.ThemeMode
import com.gorman.ourmemoryapp.ui.common.ui.SettingRow
import com.gorman.ourmemoryapp.ui.common.ui.SettingsDivider
import com.gorman.ourmemoryapp.ui.common.ui.SettingsGroup
import com.gorman.ourmemoryapp.ui.common.ui.bottomBarContentPadding
import com.gorman.ourmemoryapp.ui.common.ui.statusBarContentPadding
import com.gorman.ourmemoryapp.ui.more.models.AppLanguage
import com.gorman.ourmemoryapp.ui.more.models.MoreUiIntent
import com.gorman.ourmemoryapp.ui.more.models.labelRes
import com.gorman.ourmemoryapp.ui.more.viewmodels.MoreViewModel
import kotlinx.collections.immutable.toImmutableList

@Composable
fun MoreScreen(
    onLanguageChange: (String) -> Unit,
    onWriteToUsClick: () -> Unit,
    onMyRequestsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onScanClick: () -> Unit,
    onAdminClick: () -> Unit,
    moreViewModel: MoreViewModel = hiltViewModel()
) {
    val state by moreViewModel.uiState.collectAsStateWithLifecycle()
    val currentLanguage = AppLanguage.fromLanguage(LocalConfiguration.current.locales[0].language)
    val onUiIntent = moreViewModel::onUiIntent
    val signInWithGoogle = rememberGoogleSignInAction(onUiIntent = onUiIntent)

    LazyColumn(
        contentPadding = PaddingValues(
            start = SCREEN_PADDING,
            top = SCREEN_PADDING + statusBarContentPadding(),
            end = SCREEN_PADDING,
            bottom = SCREEN_PADDING + bottomBarContentPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Text(
                text = stringResource(R.string.more),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item { MemoryBanner() }
        item {
            AccountCard(
                account = state.account,
                signInStatus = state.signInStatus,
                onSignInClick = signInWithGoogle,
                onSignOutClick = { onUiIntent(MoreUiIntent.OnSignOutClick) }
            )
        }
        item {
            PersonalGroup(
                unseenRequestsCount = state.unseenRequestsCount,
                onMyRequestsClick = onMyRequestsClick,
                onFavoritesClick = onFavoritesClick,
                onScanClick = onScanClick,
                onWriteToUsClick = onWriteToUsClick
            )
        }
        item {
            AppearanceGroup(
                settings = state.settings,
                currentLanguage = currentLanguage,
                onUiIntent = onUiIntent,
                onLanguageChange = onLanguageChange
            )
        }
        item { RemindersGroup(settings = state.settings, onUiIntent = onUiIntent) }
        item {
            TextButton(onClick = onAdminClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.sign_in_as_admin))
            }
        }
    }
}

@Composable
private fun PersonalGroup(
    unseenRequestsCount: Int,
    onMyRequestsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onScanClick: () -> Unit,
    onWriteToUsClick: () -> Unit
) {
    SettingsGroup(title = stringResource(R.string.mine)) {
        SettingRow(
            iconRes = R.drawable.mail,
            title = stringResource(R.string.my_requests),
            onClick = onMyRequestsClick,
            trailing = { CountWithChevron(count = unseenRequestsCount) }
        )
        SettingsDivider()
        SettingRow(
            iconRes = R.drawable.favorite,
            title = stringResource(R.string.favorites),
            onClick = onFavoritesClick
        )
        SettingsDivider()
        SettingRow(
            iconRes = R.drawable.qr_code_scanner,
            title = stringResource(R.string.scan_qr_code_or_name),
            onClick = onScanClick
        )
        SettingsDivider()
        SettingRow(iconRes = R.drawable.edit, title = stringResource(R.string.write_to_us), onClick = onWriteToUsClick)
    }
}

@Composable
private fun AppearanceGroup(
    settings: AppSettings,
    currentLanguage: AppLanguage,
    onUiIntent: (MoreUiIntent) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    SettingsGroup(title = stringResource(R.string.appearance)) {
        ChoiceSettingRow(
            iconRes = R.drawable.palette,
            title = stringResource(R.string.theme),
            options = ThemeMode.entries.toImmutableList(),
            selected = settings.themeMode,
            labelRes = { it.labelRes },
            onSelect = { onUiIntent(MoreUiIntent.OnThemeModeChange(it)) }
        )
        SettingsDivider()
        ChoiceSettingRow(
            iconRes = R.drawable.text_fields,
            title = stringResource(R.string.text_size),
            options = TextScale.entries.toImmutableList(),
            selected = settings.textScale,
            labelRes = { it.labelRes },
            onSelect = { onUiIntent(MoreUiIntent.OnTextScaleChange(it)) }
        )
        SettingsDivider()
        ChoiceSettingRow(
            iconRes = R.drawable.language,
            title = stringResource(R.string.language),
            options = AppLanguage.entries.toImmutableList(),
            selected = currentLanguage,
            labelRes = { it.labelRes },
            onSelect = { if (it != currentLanguage) onLanguageChange(it.tag) }
        )
    }
}

@Composable
private fun RemindersGroup(settings: AppSettings, onUiIntent: (MoreUiIntent) -> Unit) {
    SettingsGroup(title = stringResource(R.string.reminders)) {
        SwitchSettingRow(
            iconRes = R.drawable.star,
            title = stringResource(R.string.victory_day),
            subtitle = stringResource(R.string.in_the_morning_of_may_9_msg),
            isChecked = settings.victoryDayReminder,
            onCheckedChange = { onUiIntent(MoreUiIntent.OnVictoryDayReminderChange(it)) }
        )
        SettingsDivider()
        SwitchSettingRow(
            iconRes = R.drawable.notifications,
            title = stringResource(R.string.memorable_dates_of_favorites),
            subtitle = stringResource(R.string.on_birthdays_and_memorial_days_msg),
            isChecked = settings.favoriteReminders,
            onCheckedChange = { onUiIntent(MoreUiIntent.OnFavoriteRemindersChange(it)) }
        )
    }
}

@Composable
private fun CountWithChevron(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BADGE_SPACING)) {
        if (count > 0) {
            Badge { Text(text = count.toString()) }
        }
        Icon(
            painter = painterResource(R.drawable.keyboard_arrow_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val SCREEN_PADDING = 16.dp
private val SECTION_SPACING = 20.dp
private val BADGE_SPACING = 8.dp
