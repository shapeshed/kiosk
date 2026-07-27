package com.shapeshed.kiosk.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive theme, mirroring Aerial: the platform's dynamic (wallpaper-derived)
 * colours on Android 12+, and the expressive baseline schemes otherwise. Colour is deliberately
 * personal/adaptive per the M3 philosophy rather than a fixed brand palette; the HN orange lives
 * only in the launcher icon and splash. Motion uses MaterialExpressiveTheme's expressive default.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KioskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
