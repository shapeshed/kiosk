package com.shapeshed.kiosk.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import coil3.compose.AsyncImage
import com.shapeshed.kiosk.R
import com.shapeshed.kiosk.data.ReaderArticle
import com.shapeshed.kiosk.data.ReaderBlock
import com.shapeshed.kiosk.data.ReaderFont
import com.shapeshed.kiosk.data.ReaderInline
import com.shapeshed.kiosk.data.ReaderFontSize
import com.shapeshed.kiosk.data.ReaderLineSpacing
import com.shapeshed.kiosk.data.ReaderWidth
import com.shapeshed.kiosk.data.Story
import kotlinx.coroutines.launch
import kotlin.math.ceil

internal data class ReaderPresentation(
    val fontSize: ReaderFontSize = ReaderFontSize.MEDIUM,
    val justify: Boolean = false,
    val lineSpacing: ReaderLineSpacing = ReaderLineSpacing.STANDARD,
    val width: ReaderWidth = ReaderWidth.WIDE,
) {
    // These pairs mirror Material 3's body type scale: 12/16, 14/20, 16/24, 18/28, and 22/28.
    val bodySp: Float get() = fontSize.sizeSp
    val bodyLineSp: Float
        get() = fontSize.lineHeightSp * lineSpacing.multiplier
    val textAlign: TextAlign get() = if (justify) TextAlign.Justify else TextAlign.Start
}

private val LocalReaderPresentation = compositionLocalOf { ReaderPresentation() }

// Bundled OFL reader fonts as base64 @font-face rules, so WebView reader and native reader
// typography stay comparable offline.
@Composable
internal fun rememberReaderFontFaceCss(readerFont: ReaderFont): String {
    val context = LocalContext.current
    return remember(context, readerFont) {
        when (readerFont) {
            ReaderFont.SYSTEM_SANS -> ""
            ReaderFont.NEWSREADER -> buildAssetFontFaceCss(
                family = readerFont.cssFamily,
                fonts = listOf(
                    AssetReaderFont("fonts/newsreader-400.woff2", weight = 400),
                    AssetReaderFont("fonts/newsreader-700.woff2", weight = 700),
                ),
                readBytes = { path -> context.assets.open(path).use { it.readBytes() } },
            )
            ReaderFont.LITERATA -> buildRawResourceFontFaceCss(
                family = readerFont.cssFamily,
                fonts = listOf(
                    RawReaderFont(R.font.literata, weight = 400),
                    RawReaderFont(R.font.literata, weight = 700),
                    RawReaderFont(R.font.literata_italic, weight = 400, style = "italic"),
                    RawReaderFont(R.font.literata_italic, weight = 700, style = "italic"),
                ),
                readBytes = { resId -> context.resources.openRawResource(resId).use { it.readBytes() } },
            )
            ReaderFont.ATKINSON -> buildRawResourceFontFaceCss(
                family = readerFont.cssFamily,
                fonts = listOf(
                    RawReaderFont(R.font.atkinson_hyperlegible_regular, weight = 400),
                    RawReaderFont(R.font.atkinson_hyperlegible_bold, weight = 700),
                    RawReaderFont(R.font.atkinson_hyperlegible_italic, weight = 400, style = "italic"),
                    RawReaderFont(R.font.atkinson_hyperlegible_bold_italic, weight = 700, style = "italic"),
                ),
                readBytes = { resId -> context.resources.openRawResource(resId).use { it.readBytes() } },
            )
        }
    }
}

private data class AssetReaderFont(
    val path: String,
    val weight: Int,
    val style: String = "normal",
)

private data class RawReaderFont(
    val resId: Int,
    val weight: Int,
    val style: String = "normal",
)

private fun buildAssetFontFaceCss(
    family: String,
    fonts: List<AssetReaderFont>,
    readBytes: (String) -> ByteArray,
): String = fonts.joinToString(separator = "") { font ->
    fontFaceCss(
        family = family,
        weight = font.weight,
        style = font.style,
        format = "woff2",
        bytes = readBytes(font.path),
    )
}

private fun buildRawResourceFontFaceCss(
    family: String,
    fonts: List<RawReaderFont>,
    readBytes: (Int) -> ByteArray,
): String = fonts.joinToString(separator = "") { font ->
    fontFaceCss(
        family = family,
        weight = font.weight,
        style = font.style,
        format = "truetype",
        bytes = readBytes(font.resId),
    )
}

private fun fontFaceCss(
    family: String,
    weight: Int,
    style: String,
    format: String,
    bytes: ByteArray,
): String {
    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    return "@font-face{font-family:'$family';font-style:$style;font-weight:$weight;" +
        "src:url(data:font/$format;base64,$base64) format('$format');font-display:swap;}"
}

@Composable
internal fun NativeReaderArticle(
    article: ReaderArticle,
    palette: ReaderPalette,
    readerFont: ReaderFont,
    presentation: ReaderPresentation = ReaderPresentation(),
    listState: LazyListState,
    topPad: androidx.compose.ui.unit.Dp,
    activeReadAloudBlockIndex: Int?,
    onScroll: (Int) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = remember(palette) { Color(android.graphics.Color.parseColor(palette.background)) }
    val foreground = remember(palette) { Color(android.graphics.Color.parseColor(palette.foreground)) }
    val muted = remember(palette) { Color(android.graphics.Color.parseColor(palette.muted)) }
    val link = remember(palette) { Color(android.graphics.Color.parseColor(palette.link)) }
    val rule = remember(palette) { Color(android.graphics.Color.parseColor(palette.rule)) }
    val codeBg = remember(palette) { Color(android.graphics.Color.parseColor(palette.codeBg)) }
    val readerFontFamily = readerFont.fontFamily
    val latestOnScroll by androidx.compose.runtime.rememberUpdatedState(onScroll)
    var expandedImage by remember(article) { mutableStateOf<ReaderImagePreview?>(null) }
    val articleHasNoImages = remember(article) {
        article.blocks.none { it is ReaderBlock.Image || it is ReaderBlock.Figure }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.readerScrollKey() }
            .collect { latestOnScroll(it) }
    }
    LaunchedEffect(activeReadAloudBlockIndex) {
        activeReadAloudBlockIndex?.let { blockIndex ->
            listState.animateScrollToItem(index = blockIndex + 1, scrollOffset = 48)
        }
    }

    CompositionLocalProvider(LocalReaderPresentation provides presentation) {
        SelectionContainer {
            LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .background(background),
            contentPadding = PaddingValues(start = 16.dp, top = topPad, end = 16.dp, bottom = 72.dp),
        ) {
            item(key = "header") {
                ReaderMeasure(modifier = Modifier.padding(top = 0.dp, bottom = readerLineHeightDp())) {
                    article.title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = readerFontFamily,
                                lineHeight = MaterialTheme.typography.headlineMedium.lineHeight,
                            ).readerTextMetrics(),
                            color = foreground,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    article.source?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = readerFontFamily,
                                lineHeight = LocalReaderPresentation.current.bodyLineSp.sp,
                            ).readerTextMetrics(),
                            color = muted,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(top = 0.dp),
                        )
                    }
                    // Fallback hero image: only when Readability found no image of its own — this
                    // is a substitute for a missing image, not an addition to an article that
                    // already has one.
                    if (articleHasNoImages && article.ogImageUrl != null) {
                        AsyncImage(
                            model = article.ogImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                // source carries no bottom padding of its own, so this line-height gap
                                // supplies the full separation before the image.
                                .padding(top = readerLineHeightDp())
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { expandedImage = ReaderImagePreview(article.ogImageUrl, null) },
                        )
                    }
                }
            }
            itemsIndexed(article.blocks, key = { index, _ -> "block-$index" }) { index, block ->
                val active = activeReadAloudBlockIndex == index
                ReaderMeasure(
                    modifier = if (active) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBg)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    } else {
                        Modifier
                    },
                ) {
                    ReaderBlockView(
                        block = block,
                        foreground = foreground,
                        muted = muted,
                        link = link,
                        rule = rule,
                        codeBg = codeBg,
                        readerFontFamily = readerFontFamily,
                        onOpenLink = onOpenLink,
                        onOpenImage = { src, alt -> expandedImage = ReaderImagePreview(src, alt) },
                    )
                }
            }
            }
        }
    }
    expandedImage?.let { image ->
        ZoomableImageOverlay(
            image = image,
            background = background,
            onDismiss = { expandedImage = null },
        )
    }
}

private data class ReaderImagePreview(
    val src: String,
    val alt: String?,
)

internal fun LazyListState.readerScrollKey(): Int =
    firstVisibleItemIndex * 100_000 + firstVisibleItemScrollOffset

@Composable
private fun ReaderMeasure(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LocalReaderPresentation.current.width.marginDp.dp),
    ) {
        content()
    }
}

@Composable
private fun ReaderBlockView(
    block: ReaderBlock,
    foreground: Color,
    muted: Color,
    link: Color,
    rule: Color,
    codeBg: Color,
    readerFontFamily: FontFamily,
    onOpenLink: (String) -> Unit,
    onOpenImage: (src: String, alt: String?) -> Unit,
) {
    when (block) {
        is ReaderBlock.Heading -> {
            val headingStyle = when (block.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                5 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            val headingText = readerAnnotatedString(block.text, foreground, link, codeBg, readerFontFamily)
            val headingLineHeightSp = headingStyle.lineHeight.value
            val headingTextStyle = headingStyle.copy(
                fontFamily = readerFontFamily,
                lineHeight = headingLineHeightSp.sp,
            ).readerTextMetrics()
            val textMeasurer = rememberTextMeasurer()
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
                contentAlignment = Alignment.Center,
            ) {
                val maxWidthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
                val lineCount = remember(headingText, headingTextStyle, maxWidthPx) {
                    textMeasurer.measure(
                        text = headingText,
                        style = headingTextStyle,
                        constraints = Constraints(maxWidth = maxWidthPx),
                    ).lineCount
                }
                val headingUnits = ceil(
                    lineCount * headingLineHeightSp / LocalReaderPresentation.current.bodyLineSp,
                ).toInt().coerceAtLeast(1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(readerLineHeightDp() * headingUnits),
                    contentAlignment = Alignment.Center,
                ) {
                    ReaderText(
                        text = headingText,
                        style = headingTextStyle,
                        color = foreground,
                        modifier = Modifier.fillMaxWidth(),
                        onOpenLink = onOpenLink,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
        is ReaderBlock.Paragraph -> ReaderText(
            text = readerAnnotatedString(block.text, foreground, link, codeBg, readerFontFamily),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = readerFontFamily,
                fontSize = LocalReaderPresentation.current.bodySp.sp,
                lineHeight = LocalReaderPresentation.current.bodyLineSp.sp,
            ),
            color = foreground,
            modifier = Modifier.padding(bottom = readerLineHeightDp()),
            onOpenLink = onOpenLink,
        )
        is ReaderBlock.Quote -> Row(
            Modifier
                .padding(top = 0.dp, bottom = readerLineHeightDp())
                .height(IntrinsicSize.Min),
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(rule),
            )
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                block.blocks.forEach {
                    ReaderBlockView(
                        block = it,
                        foreground = muted,
                        muted = muted,
                        link = link,
                        rule = rule,
                        codeBg = codeBg,
                        readerFontFamily = readerFontFamily,
                        onOpenLink = onOpenLink,
                        onOpenImage = onOpenImage,
                    )
                }
            }
        }
        is ReaderBlock.CodeBlock -> Text(
            text = block.text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = LocalReaderPresentation.current.bodySp.sp,
                lineHeight = LocalReaderPresentation.current.bodyLineSp.sp,
            ).readerTextMetrics(),
            color = foreground,
            modifier = Modifier
                .padding(bottom = readerLineHeightDp())
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(codeBg)
                .padding(12.dp),
        )
        is ReaderBlock.BulletedList -> ReaderList(
            items = block.items,
            ordered = false,
            foreground = foreground,
            link = link,
            codeBg = codeBg,
            readerFontFamily = readerFontFamily,
            onOpenLink = onOpenLink,
        )
        is ReaderBlock.NumberedList -> ReaderList(
            items = block.items,
            ordered = true,
            foreground = foreground,
            link = link,
            codeBg = codeBg,
            readerFontFamily = readerFontFamily,
            onOpenLink = onOpenLink,
        )
        is ReaderBlock.Table -> ReaderTable(
            rows = block.rows,
            foreground = foreground,
            link = link,
            rule = rule,
            codeBg = codeBg,
            readerFontFamily = readerFontFamily,
            onOpenLink = onOpenLink,
        )
        is ReaderBlock.Image -> AsyncImage(
            model = block.src,
            contentDescription = block.alt,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .padding(bottom = readerLineHeightDp())
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenImage(block.src, block.alt) },
        )
        is ReaderBlock.Figure -> ReaderFigure(block, muted, link, codeBg, readerFontFamily, onOpenLink, onOpenImage)
        ReaderBlock.Divider -> HorizontalDivider(
            color = rule,
            modifier = Modifier.padding(top = 0.dp, bottom = readerLineHeightDp()),
        )
    }
}

@Composable
private fun ReaderTable(
    rows: List<List<List<ReaderInline>>>,
    foreground: Color,
    link: Color,
    rule: Color,
    codeBg: Color,
    readerFontFamily: FontFamily,
    onOpenLink: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = readerLineHeightDp())
            .border(1.dp, rule, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (rowIndex == 0) codeBg else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = readerLineHeightDp() / 2f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { cell ->
                    ReaderText(
                        text = readerAnnotatedString(cell, foreground, link, codeBg, readerFontFamily),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = readerFontFamily,
                            fontSize = LocalReaderPresentation.current.bodySp.sp,
                            lineHeight = LocalReaderPresentation.current.bodyLineSp.sp,
                            fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = foreground,
                        modifier = Modifier.weight(1f),
                        onOpenLink = onOpenLink,
                    )
                }
            }
            if (rowIndex < rows.lastIndex) HorizontalDivider(color = rule)
        }
    }
}

@Composable
private fun ReaderFigure(
    figure: ReaderBlock.Figure,
    muted: Color,
    link: Color,
    codeBg: Color,
    readerFontFamily: FontFamily,
    onOpenLink: (String) -> Unit,
    onOpenImage: (src: String, alt: String?) -> Unit,
) {
    Column(Modifier.padding(top = 0.dp, bottom = readerLineHeightDp())) {
        figure.images.forEachIndexed { index, image ->
            AsyncImage(
                model = image.src,
                contentDescription = image.alt,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .padding(bottom = if (index == figure.images.lastIndex) 0.dp else readerLineHeightDp())
                    .fillMaxWidth()
                    .heightIn(min = 1.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenImage(image.src, image.alt) },
            )
        }
        figure.caption?.let { caption ->
            ReaderText(
                text = readerAnnotatedString(caption, muted, link, codeBg, readerFontFamily),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = readerFontFamily,
                ),
                color = muted,
                modifier = Modifier.padding(top = readerLineHeightDp()),
                onOpenLink = onOpenLink,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun ZoomableImageOverlay(
    image: ReaderImagePreview,
    background: Color,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scale = remember(image.src) { Animatable(1f) }
    var offsetX by remember(image.src) { mutableFloatStateOf(0f) }
    var offsetY by remember(image.src) { mutableFloatStateOf(0f) }

    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(background.copy(alpha = 0.98f))
            .pointerInput(image.src) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale.value * zoom).coerceIn(1f, 5f)
                    scope.launch { scale.snapTo(nextScale) }
                    if (nextScale == 1f) {
                        offsetX = 0f
                        offsetY = 0f
                    } else {
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
            }
            .pointerInput(image.src) {
                detectTapGestures(
                    onDoubleTap = {
                        offsetX = 0f
                        offsetY = 0f
                        scope.launch {
                            scale.animateTo(
                                targetValue = if (scale.value > 1f) 1f else 2.5f,
                                animationSpec = tween(180),
                            )
                        }
                    },
                    onTap = { onDismiss() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = image.src,
            contentDescription = image.alt,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = offsetX
                    translationY = offsetY
                },
        )
    }
}

@Composable
private fun ReaderText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    var layoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val linkModifier = modifier.pointerInput(text) {
        detectTapGestures { position ->
            val offset = layoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
            val url = text.getStringAnnotations(tag = ReaderUrlAnnotation, start = offset, end = offset)
                .firstOrNull()
                ?.item
                ?: return@detectTapGestures
            onOpenLink(url)
        }
    }
    Text(
        text = text,
        style = style.readerTextMetrics().copy(
            textAlign = textAlign ?: LocalReaderPresentation.current.textAlign,
        ),
        color = color,
        modifier = linkModifier,
        onTextLayout = {
            layoutResult = it
            onTextLayout?.invoke(it)
        },
    )
}

private fun TextStyle.readerTextMetrics(): TextStyle = copy(
    // Android's legacy font padding makes the first and last line depend on font
    // metrics rather than the declared line height. Remove it so every reader
    // block uses the same explicit line box.
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Proportional,
        trim = LineHeightStyle.Trim.None,
    ),
)

private val NewsreaderFontFamily = FontFamily(
    Font(
        R.font.newsreader,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.newsreader,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.newsreader,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        R.font.newsreader_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(400), FontVariation.italic(1f)),
    ),
    Font(
        R.font.newsreader_italic,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.italic(1f)),
    ),
    Font(
        R.font.newsreader_italic,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.italic(1f)),
    ),
)

private val LiterataFontFamily = FontFamily(
    Font(
        R.font.literata,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.literata,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.literata,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        R.font.literata_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(400), FontVariation.italic(1f)),
    ),
    Font(
        R.font.literata_italic,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.italic(1f)),
    ),
    Font(
        R.font.literata_italic,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.italic(1f)),
    ),
)

private val AtkinsonHyperlegibleFontFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, weight = FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, weight = FontWeight.Bold),
    Font(R.font.atkinson_hyperlegible_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(R.font.atkinson_hyperlegible_bold_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
)

internal val ReaderFont.fontFamily: FontFamily
    get() = when (this) {
        ReaderFont.NEWSREADER -> NewsreaderFontFamily
        ReaderFont.LITERATA -> LiterataFontFamily
        ReaderFont.ATKINSON -> AtkinsonHyperlegibleFontFamily
        ReaderFont.SYSTEM_SANS -> FontFamily.SansSerif
    }

private val ReaderFont.cssFamily: String
    get() = when (this) {
        ReaderFont.NEWSREADER -> "Newsreader"
        ReaderFont.LITERATA -> "Literata"
        ReaderFont.ATKINSON -> "Atkinson Hyperlegible"
        ReaderFont.SYSTEM_SANS -> "System Sans"
    }

@Composable
private fun ReaderList(
    items: List<List<ReaderInline>>,
    ordered: Boolean,
    foreground: Color,
    link: Color,
    codeBg: Color,
    readerFontFamily: FontFamily,
    onOpenLink: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = readerLineHeightDp()),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (ordered) "${index + 1}." else "•",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = readerFontFamily,
                        fontSize = LocalReaderPresentation.current.bodySp.sp,
                        lineHeight = LocalReaderPresentation.current.bodyLineSp.sp,
                    ).readerTextMetrics(),
                    color = foreground,
                    modifier = Modifier.alignBy(FirstBaseline),
                )
                ReaderText(
                    text = readerAnnotatedString(item, foreground, link, codeBg, readerFontFamily),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = readerFontFamily,
                            fontSize = LocalReaderPresentation.current.bodySp.sp,
                            lineHeight = LocalReaderPresentation.current.bodyLineSp.sp,
                        ).readerTextMetrics(),
                    color = foreground,
                    modifier = Modifier
                        .weight(1f)
                        .alignBy(FirstBaseline),
                    onOpenLink = onOpenLink,
                )
            }
        }
    }
}

@Composable
private fun readerLineHeightDp(): androidx.compose.ui.unit.Dp {
    val lineHeight = LocalReaderPresentation.current.bodyLineSp
    return with(LocalDensity.current) {
        lineHeight.sp.toDp()
    }
}

private fun readerAnnotatedString(
    inlines: List<ReaderInline>,
    foreground: Color,
    link: Color,
    codeBg: Color,
    readerFontFamily: FontFamily,
): AnnotatedString = buildAnnotatedString {
    inlines.forEach { inline ->
        val style = SpanStyle(
            color = if (inline.href != null) link else foreground,
            fontFamily = if (inline.code) FontFamily.Monospace else readerFontFamily,
            fontSize = if (inline.code) 17.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
            fontWeight = if (inline.strong) FontWeight.Bold else null,
            fontStyle = if (inline.emphasis) FontStyle.Italic else null,
            background = Color.Unspecified,
            textDecoration = TextDecoration.None,
        )
        if (inline.href == null) {
            withStyle(style) { append(inline.text) }
        } else {
            pushStringAnnotation(tag = ReaderUrlAnnotation, annotation = inline.href)
            withStyle(style) { append(inline.text) }
            pop()
        }
    }
}

private const val ReaderUrlAnnotation = "reader-url"

@Composable
internal fun TextPost(
    story: Story,
    palette: ReaderPalette,
    readerFont: ReaderFont,
    presentation: ReaderPresentation = ReaderPresentation(),
    listState: LazyListState,
    topPad: androidx.compose.ui.unit.Dp,
    onScroll: (Int) -> Unit,
) {
    val background = remember(palette) { Color(android.graphics.Color.parseColor(palette.background)) }
    val foreground = remember(palette) { Color(android.graphics.Color.parseColor(palette.foreground)) }
    val muted = remember(palette) { Color(android.graphics.Color.parseColor(palette.muted)) }
    val rule = remember(palette) { Color(android.graphics.Color.parseColor(palette.rule)) }
    val readerFontFamily = readerFont.fontFamily
    val latestOnScroll by androidx.compose.runtime.rememberUpdatedState(onScroll)
    LaunchedEffect(listState) {
        snapshotFlow { listState.readerScrollKey() }
            .collect { latestOnScroll(it) }
    }

    CompositionLocalProvider(LocalReaderPresentation provides presentation) {
        SelectionContainer {
            LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(background),
            contentPadding = PaddingValues(start = 16.dp, top = topPad, end = 16.dp, bottom = 72.dp),
        ) {
            item(key = "text-post") {
                ReaderMeasure(modifier = Modifier.padding(top = 0.dp, bottom = readerLineHeightDp())) {
                    Text(
                        text = story.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = readerFontFamily,
                            lineHeight = MaterialTheme.typography.headlineMedium.lineHeight,
                        ).readerTextMetrics(),
                        color = foreground,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = story.by,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = readerFontFamily,
                            lineHeight = LocalReaderPresentation.current.bodyLineSp.sp,
                        ).readerTextMetrics(),
                        color = muted,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 0.dp),
                    )
                    story.text?.takeIf { it.isNotBlank() }?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = readerFontFamily,
                                fontSize = LocalReaderPresentation.current.bodySp.sp,
                                lineHeight = LocalReaderPresentation.current.bodyLineSp.sp,
                            ).readerTextMetrics(),
                            color = foreground,
                            textAlign = LocalReaderPresentation.current.textAlign,
                            modifier = Modifier.padding(top = 32.dp),
                        )
                    }
                }
            }
            }
        }
    }
}
