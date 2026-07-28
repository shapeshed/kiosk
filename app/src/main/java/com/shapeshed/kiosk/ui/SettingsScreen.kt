package com.shapeshed.kiosk.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shapeshed.kiosk.KioskApp
import com.shapeshed.kiosk.R
import com.shapeshed.kiosk.data.DefaultViewer
import com.shapeshed.kiosk.data.ReaderFont
import com.shapeshed.kiosk.data.ReaderTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as KioskApp
    val scope = rememberCoroutineScope()
    val defaultViewer by app.settings.defaultViewer.collectAsStateWithLifecycle(DefaultViewer.READER)
    val readerTheme by app.settings.readerTheme.collectAsStateWithLifecycle(ReaderTheme.SYSTEM)
    val readerFont by app.settings.readerFont.collectAsStateWithLifecycle(ReaderFont.NEWSREADER)

    BackHandler(enabled = showBack, onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
                windowInsets = WindowInsets.statusBars,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "viewer") {
                SettingsSection(title = stringResource(R.string.default_viewer)) {
                    SettingChoiceRow(
                        title = stringResource(R.string.reader_view),
                        supportingText = stringResource(R.string.default_viewer_reader_summary),
                        selected = defaultViewer == DefaultViewer.READER,
                        icon = { Icon(Icons.AutoMirrored.Filled.Article, null) },
                        onClick = { scope.launch { app.settings.setDefaultViewer(DefaultViewer.READER) } },
                    )
                    HorizontalDivider()
                    SettingChoiceRow(
                        title = stringResource(R.string.web_view),
                        supportingText = stringResource(R.string.default_viewer_web_summary),
                        selected = defaultViewer == DefaultViewer.WEB,
                        icon = { Icon(Icons.Filled.Public, null) },
                        onClick = { scope.launch { app.settings.setDefaultViewer(DefaultViewer.WEB) } },
                    )
                }
            }

            item(key = "theme") {
                SettingsSection(title = stringResource(R.string.reader_theme)) {
                    ReaderTheme.entries.forEachIndexed { index, theme ->
                        SettingChoiceRow(
                            title = readerThemeLabelForSettings(theme),
                            selected = readerTheme == theme,
                            icon = if (index == 0) {
                                { Icon(Icons.Filled.FormatSize, null) }
                            } else {
                                null
                            },
                            onClick = { scope.launch { app.settings.setReaderTheme(theme) } },
                        )
                        if (index != ReaderTheme.entries.lastIndex) HorizontalDivider()
                    }
                }
            }

            item(key = "font") {
                SettingsSection(title = stringResource(R.string.reader_font)) {
                    ReaderFont.entries.forEachIndexed { index, font ->
                        SettingChoiceRow(
                            title = font.labelForSettings,
                            selected = readerFont == font,
                            onClick = { scope.launch { app.settings.setReaderFont(font) } },
                        )
                        if (index != ReaderFont.entries.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun SettingChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        supportingContent = supportingText?.let { text -> { Text(text) } },
        leadingContent = icon,
        trailingContent = {
            RadioButton(selected = selected, onClick = null)
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Text(
            text = title,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun readerThemeLabelForSettings(theme: ReaderTheme): String = stringResource(
    when (theme) {
        ReaderTheme.SYSTEM -> R.string.theme_system
        ReaderTheme.LIGHT -> R.string.theme_light
        ReaderTheme.DARK -> R.string.theme_dark
        ReaderTheme.SEPIA -> R.string.theme_sepia
    },
)

private val ReaderFont.labelForSettings: String
    get() = when (this) {
        ReaderFont.NEWSREADER -> "Newsreader"
        ReaderFont.LITERATA -> "Literata"
        ReaderFont.ATKINSON -> "Atkinson Hyperlegible"
        ReaderFont.SYSTEM_SANS -> "System Sans"
    }
