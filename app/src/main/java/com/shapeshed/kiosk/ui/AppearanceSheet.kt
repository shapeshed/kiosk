package com.shapeshed.kiosk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FormatAlignJustify
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shapeshed.kiosk.R
import com.shapeshed.kiosk.data.ReaderFont
import com.shapeshed.kiosk.data.ReaderFontSize
import com.shapeshed.kiosk.data.ReaderLineSpacing
import com.shapeshed.kiosk.data.ReaderWidth
import com.shapeshed.kiosk.data.ReaderTheme

/** Instapaper-style swipe-up appearance panel: theme swatches and reader font choices. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceSheet(
    current: ReaderTheme,
    currentFont: ReaderFont,
    currentFontSize: ReaderFontSize,
    currentJustify: Boolean,
    currentLineSpacing: ReaderLineSpacing,
    currentWidth: ReaderWidth,
    onSelectTheme: (ReaderTheme) -> Unit,
    onSelectFont: (ReaderFont) -> Unit,
    onSelectFontSize: (ReaderFontSize) -> Unit,
    onSelectJustify: (Boolean) -> Unit,
    onSelectLineSpacing: (ReaderLineSpacing) -> Unit,
    onSelectWidth: (ReaderWidth) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            val pagerState = rememberPagerState(pageCount = { 2 })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                pageSpacing = 20.dp,
                overscrollEffect = null,
            ) { page ->
                Column(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (page) {
                        0 -> {
                            SettingLabel("Theme")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(ReaderTheme.SYSTEM, ReaderTheme.LIGHT, ReaderTheme.SEPIA, ReaderTheme.DARK).forEach { theme ->
                                    ThemeSwatch(
                                        fill = swatchBrush(theme),
                                        label = readerThemeLabel(theme),
                                        selected = current == theme,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onSelectTheme(theme) },
                                    )
                                }
                            }
                            SettingLabel("Font")
                            FontPicker(current = currentFont, onSelect = onSelectFont)
                            StepperRow("Font size", ReaderFontSize.entries, currentFontSize, onSelectFontSize, Icons.Rounded.Remove, Icons.Rounded.Add)
                        }
                        else -> {
                            IconOptionRow("Justify text", currentJustify, false, true, onSelectJustify, Icons.AutoMirrored.Rounded.FormatAlignLeft, Icons.Rounded.FormatAlignJustify)
                            StepperRow("Line height", ReaderLineSpacing.entries, currentLineSpacing, onSelectLineSpacing, Icons.Rounded.Remove, Icons.Rounded.Add)
                            StepperRow("Page margins", ReaderWidth.entries, currentWidth, onSelectWidth, Icons.Rounded.Remove, Icons.Rounded.Add)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(2) { page ->
                    Text(
                        text = if (page == pagerState.currentPage) "●" else "○",
                        color = if (page == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FontPicker(
    current: ReaderFont,
    onSelect: (ReaderFont) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(current.label, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = current.fontFamily))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ReaderFont.entries.forEach { font ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(font.label, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = font.fontFamily))
                        }
                    },
                    onClick = {
                        onSelect(font)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun <T> StepperRow(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    decrementIcon: androidx.compose.ui.graphics.vector.ImageVector,
    incrementIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val index = options.indexOf(selected).coerceAtLeast(0)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.FilledTonalIconButton(enabled = index > 0, onClick = { onSelect(options[index - 1]) }) {
                Icon(decrementIcon, contentDescription = "Decrease $label")
            }
            androidx.compose.material3.FilledTonalIconButton(enabled = index < options.lastIndex, onClick = { onSelect(options[index + 1]) }) {
                Icon(incrementIcon, contentDescription = "Increase $label")
            }
        }
    }
}

@Composable
private fun <T> IconOptionRow(
    label: String,
    selected: T,
    first: T,
    second: T,
    onSelect: (T) -> Unit,
    firstIcon: androidx.compose.ui.graphics.vector.ImageVector,
    secondIcon: androidx.compose.ui.graphics.vector.ImageVector,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            leadingIcon?.let { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(first to firstIcon, second to secondIcon).forEach { (option, icon) ->
                Surface(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected == option) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (selected == option) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> OptionRow(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
) {
    Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(optionLabel(option)) },
            )
        }
    }
}

@Composable
internal fun FontChoiceRow(
    font: ReaderFont,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = font.label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = font.fontFamily,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

private val ReaderFont.label: String
    get() = when (this) {
        ReaderFont.NEWSREADER -> "Newsreader"
        ReaderFont.LITERATA -> "Literata"
        ReaderFont.ATKINSON -> "Atkinson Hyperlegible"
        ReaderFont.SYSTEM_SANS -> "System Sans"
    }

@Composable
internal fun ThemeSwatch(
    fill: Brush,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(fill)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
    )
}

internal fun swatchBrush(theme: ReaderTheme): Brush = when (theme) {
    ReaderTheme.LIGHT -> SolidColor(Color(0xFFFDFDFB))
    ReaderTheme.SEPIA -> SolidColor(Color(0xFFF4ECD8))
    ReaderTheme.DARK -> SolidColor(Color(0xFF16161A))
    ReaderTheme.SYSTEM -> Brush.horizontalGradient(
        0f to Color(0xFFFDFDFB),
        0.5f to Color(0xFFFDFDFB),
        0.5f to Color(0xFF16161A),
        1f to Color(0xFF16161A),
    )
}

@Composable
internal fun readerThemeLabel(theme: ReaderTheme): String = stringResource(
    when (theme) {
        ReaderTheme.SYSTEM -> R.string.theme_system
        ReaderTheme.LIGHT -> R.string.theme_light
        ReaderTheme.DARK -> R.string.theme_dark
        ReaderTheme.SEPIA -> R.string.theme_sepia
    },
)

@Composable
internal fun AppearanceGlyph(modifier: Modifier = Modifier) {
    Text(
        text = "Aa",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
