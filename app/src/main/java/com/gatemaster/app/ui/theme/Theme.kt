package com.gatemaster.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = IndigoOnPrimaryLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = IndigoOnContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    secondaryContainer = SlateContainerLight,
    onSecondaryContainer = SlateOnContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = AmberOnTertiaryLight,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = AmberOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
)

private val DarkScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoOnContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateContainerDark,
    onSecondaryContainer = SlateOnContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = AmberOnTertiaryDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)

/**
 * Answer-state colours resolved for the active theme. Kept out of
 * [MaterialTheme.colorScheme] because they are semantic, not brand: they must
 * stay recognisable even when dynamic colour repaints everything else.
 */
data class GateAnswerColors(
    val correct: Color,
    val incorrect: Color,
    val marked: Color,
    val unanswered: Color,
)

val LocalAnswerColors = staticCompositionLocalOf {
    GateAnswerColors(
        correct = AnswerColors.CorrectLight,
        incorrect = AnswerColors.IncorrectLight,
        marked = AnswerColors.MarkedLight,
        unanswered = AnswerColors.UnansweredLight,
    )
}

@Composable
fun GateMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You wallpaper colours, available from Android 12.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    val answerColors = if (darkTheme) {
        GateAnswerColors(
            correct = AnswerColors.CorrectDark,
            incorrect = AnswerColors.IncorrectDark,
            marked = AnswerColors.MarkedDark,
            unanswered = AnswerColors.UnansweredDark,
        )
    } else {
        GateAnswerColors(
            correct = AnswerColors.CorrectLight,
            incorrect = AnswerColors.IncorrectLight,
            marked = AnswerColors.MarkedLight,
            unanswered = AnswerColors.UnansweredLight,
        )
    }

    CompositionLocalProvider(LocalAnswerColors provides answerColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GateTypography,
            content = content,
        )
    }
}
