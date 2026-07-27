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

/**
 * Wrap the article HTML that Readability extracts in a minimal, Instapaper-style reader document:
 * a serif measure, generous line height, a muted source line, and quiet link colour. Pure so it
 * can be unit-tested; no Android dependencies.
 */
fun buildReaderHtml(
    title: String?,
    source: String?,
    contentHtml: String,
    palette: ReaderPalette,
    fontFaceCss: String = "",
    readerFontFamily: String = "Newsreader",
    topPadPx: Int = 8,
): String {
    val heading = title?.takeIf { it.isNotBlank() }?.let { "<h1>${escapeHtml(it)}</h1>" }.orEmpty()
    val sourceLine = source?.takeIf { it.isNotBlank() }?.let { "<p class=\"src\">${escapeHtml(it)}</p>" }.orEmpty()
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
        <style>
          $fontFaceCss
          :root { color-scheme: ${if (palette.dark) "dark" else "light"}; }
          html, body { margin: 0; }
          body {
            background: ${palette.background};
            color: ${palette.foreground};
            font-family: $readerFontFamily, Georgia, Charter, 'Times New Roman', 'Noto Serif', serif;
            font-size: 20px;
            line-height: 1.7;
            max-width: 38rem;
            margin: 0 auto;
            padding: ${topPadPx}px 24px 72px;
            overflow-wrap: break-word;
            text-rendering: optimizeLegibility;
          }
          h1 { font-size: 2rem; line-height: 1.2; font-weight: 700; margin: 0.4em 0 0.1em; }
          h2, h3 { line-height: 1.3; margin-top: 1.6em; }
          p { margin: 0 0 1.1em; }
          p.src { color: ${palette.muted}; font-size: 1rem; margin: 0 0 1.6em; }
          a { color: ${palette.link}; text-decoration: none; }
          img, figure, video { max-width: 100%; height: auto; }
          figure { margin: 1.4rem 0; }
          figcaption { color: ${palette.muted}; font-size: 0.85rem; }
          blockquote {
            border-left: 3px solid ${palette.rule}; margin: 1.2rem 0; padding: 0 0 0 1rem;
            color: ${palette.muted};
          }
          hr { border: none; border-top: 1px solid ${palette.rule}; margin: 2rem 0; }
          pre, code {
            font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 0.85rem;
          }
          pre {
            background: ${palette.codeBg}; padding: 12px; border-radius: 8px; overflow-x: auto;
            white-space: pre-wrap; word-break: break-word; line-height: 1.45;
          }
        </style>
        </head>
        <body>$heading$sourceLine$contentHtml</body>
        </html>
    """.trimIndent()
}

private fun escapeHtml(text: String): String =
    text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
