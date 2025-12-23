package com.example.recipebuilder.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,            // used for titles, main accents
    onPrimary = Color.White,
    secondary = SkyBlueDark,       // used for icons and highlights
    onSecondary = Color.White,
    background = SkyBlueLight,     // general background
    onBackground = NavyBlue,       // text on background
    surface = PureWhite,           // cards, menus
    onSurface = CardBrown,         // text inside cards
    outline = BorderBlue,          // borders
    error = Color(0xFFD32F2F)
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = Color.Black,
    secondary = SkyBlueMedium,
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3F2FD),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEEEEEE),
    outline = SkyBlueDark
)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */


@Composable
fun RecipeBuilderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}