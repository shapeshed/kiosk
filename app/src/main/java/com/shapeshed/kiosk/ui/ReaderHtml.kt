package com.shapeshed.kiosk.ui

import com.shapeshed.kiosk.data.ReaderTheme

/** Colours for the reader, as CSS hex strings, plus whether it's a dark scheme. */
data class ReaderPalette(
    val background: String,
    val foreground: String,
    val muted: String,
    val link: String,
    val rule: String,
    val codeBg: String,
    val dark: Boolean,
)

private val lightPalette = ReaderPalette("#FDFDFB", "#1A1A1A", "#6B6B6B", "#1A66C2", "#E6E4DF", "#F2F1EC", dark = false)
private val darkPalette = ReaderPalette("#16161A", "#E7E4DE", "#9B9792", "#8AB4F8", "#2A2A30", "#1F1F25", dark = true)
private val sepiaPalette = ReaderPalette("#F4ECD8", "#5B4636", "#8A7A5C", "#1A5FB4", "#E4D9BC", "#EBE0C4", dark = false)

/** Resolve the reader colours for a [theme]; SYSTEM follows [systemDark]. */
fun readerPaletteFor(theme: ReaderTheme, systemDark: Boolean): ReaderPalette = when (theme) {
    ReaderTheme.LIGHT -> lightPalette
    ReaderTheme.DARK -> darkPalette
    ReaderTheme.SEPIA -> sepiaPalette
    ReaderTheme.SYSTEM -> if (systemDark) darkPalette else lightPalette
}

