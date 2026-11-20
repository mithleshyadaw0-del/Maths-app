package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class MbsExtendedColors(
    val navy: Color = NavyDeep,
    val navyCard: Color = NavyCard,
    val gold: Color = GoldenYellow,
    val goldSoft: Color = GoldenSoft,
    val mintTint: Color = MintTint,
    val mintAccent: Color = MintAccent,
    val indigoTint: Color = IndigoTint,
    val indigoAccent: Color = IndigoAccent,
    val amberTint: Color = AmberTint,
    val amberAccent: Color = AmberAccent,
    val pinkTint: Color = PinkTint,
    val pinkAccent: Color = PinkAccent,
    val coralTint: Color = CoralTint,
    val coralAccent: Color = CoralAccent,
    val reviewPurple: Color = ReviewPurple,
    val reviewPurpleTint: Color = ReviewPurpleTint,
    val neutralPill: Color = NeutralPill,
    val textMuted: Color = TextMuted,
    val cardBorder: Color = OutlineSubtle
)

val LocalMbsColors = staticCompositionLocalOf { MbsExtendedColors() }

private val MbsLightColorScheme = lightColorScheme(
    primary = NavyDeep,
    onPrimary = Color.White,
    primaryContainer = IndigoTint,
    onPrimaryContainer = NavyDeep,
    secondary = GoldenYellow,
    onSecondary = NavyDeep,
    secondaryContainer = GoldenSoft,
    onSecondaryContainer = NavyDeep,
    tertiary = MintAccent,
    onTertiary = Color.White,
    tertiaryContainer = MintTint,
    onTertiaryContainer = MintDark,
    background = LavenderBg,
    onBackground = NavyDeep,
    surface = SurfaceWhite,
    onSurface = NavyDeep,
    surfaceVariant = Color(0xFFECEAF8),
    onSurfaceVariant = TextSecondary,
    outline = OutlineSubtle,
    error = CoralAccent,
    onError = Color.White,
    errorContainer = CoralTint,
    onErrorContainer = CoralAccent
)

private val MbsDarkColorScheme = darkColorScheme(
    primary = GoldenYellow,
    onPrimary = NavyDeep,
    primaryContainer = NavyCard,
    onPrimaryContainer = Color.White,
    secondary = IndigoAccent,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = MintAccent,
    onTertiary = NavyDeep,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF343866),
    error = Color(0xFFFF6B6B),
    onError = NavyDeep
)

private val MbsShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MbsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MbsDarkColorScheme else MbsLightColorScheme
    val extendedColors = if (darkTheme) {
        MbsExtendedColors(
            navy = NavyDeep,
            navyCard = Color(0xFF1F234A),
            gold = GoldenYellow,
            goldSoft = Color(0xFF3A311C),
            mintTint = Color(0xFF16352A),
            mintAccent = MintAccent,
            indigoTint = Color(0xFF21264D),
            indigoAccent = Color(0xFF7B88FF),
            amberTint = Color(0xFF382D18),
            amberAccent = GoldenYellow,
            pinkTint = Color(0xFF3A1D2C),
            pinkAccent = Color(0xFFFF6CA0),
            coralTint = Color(0xFF3C1E1E),
            coralAccent = Color(0xFFFF6B6B),
            reviewPurple = Color(0xFFA685FF),
            reviewPurpleTint = Color(0xFF2A214A),
            neutralPill = Color(0xFF252950),
            textMuted = DarkTextSecondary,
            cardBorder = Color(0xFF343866)
        )
    } else {
        MbsExtendedColors()
    }

    CompositionLocalProvider(LocalMbsColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MbsTypography,
            shapes = MbsShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) = MbsTheme(darkTheme = darkTheme, content = content)
