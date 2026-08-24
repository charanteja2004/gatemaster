package com.gatemaster.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette. Deep indigo reads as "serious study tool" without the
 * saturated blue every ed-tech app defaults to; the amber accent is reserved
 * for streaks, progress and anything the user earns.
 */

// Light
val IndigoPrimaryLight = Color(0xFF2B4BB8)
val IndigoOnPrimaryLight = Color(0xFFFFFFFF)
val IndigoContainerLight = Color(0xFFDDE1FF)
val IndigoOnContainerLight = Color(0xFF001453)

val SlateSecondaryLight = Color(0xFF5A5D72)
val SlateOnSecondaryLight = Color(0xFFFFFFFF)
val SlateContainerLight = Color(0xFFDFE1F9)
val SlateOnContainerLight = Color(0xFF171B2C)

val AmberTertiaryLight = Color(0xFF8A5100)
val AmberOnTertiaryLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFFDDB6)
val AmberOnTertiaryContainerLight = Color(0xFF2C1600)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val SurfaceLight = Color(0xFFFBF8FF)
val OnSurfaceLight = Color(0xFF1B1B21)
val SurfaceVariantLight = Color(0xFFE3E1EC)
val OnSurfaceVariantLight = Color(0xFF46464F)
val OutlineLight = Color(0xFF767680)

// Dark
val IndigoPrimaryDark = Color(0xFFB8C3FF)
val IndigoOnPrimaryDark = Color(0xFF002585)
val IndigoContainerDark = Color(0xFF0D359E)
val IndigoOnContainerDark = Color(0xFFDDE1FF)

val SlateSecondaryDark = Color(0xFFC3C5DD)
val SlateOnSecondaryDark = Color(0xFF2C2F42)
val SlateContainerDark = Color(0xFF424659)
val SlateOnContainerDark = Color(0xFFDFE1F9)

val AmberTertiaryDark = Color(0xFFFFB868)
val AmberOnTertiaryDark = Color(0xFF4A2800)
val AmberContainerDark = Color(0xFF693C00)
val AmberOnTertiaryContainerDark = Color(0xFFFFDDB6)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val SurfaceDark = Color(0xFF121318)
val OnSurfaceDark = Color(0xFFE4E1E9)
val SurfaceVariantDark = Color(0xFF46464F)
val OnSurfaceVariantDark = Color(0xFFC7C5D0)
val OutlineDark = Color(0xFF90909A)

/** Semantic colours for answer state. Deliberately not the brand accent. */
object AnswerColors {
    val CorrectLight = Color(0xFF1B7A57)
    val CorrectDark = Color(0xFF6EDBAF)
    val IncorrectLight = Color(0xFFB3261E)
    val IncorrectDark = Color(0xFFF2B8B5)
    val MarkedLight = Color(0xFF7B3FB8)
    val MarkedDark = Color(0xFFD5BAFF)
    val UnansweredLight = Color(0xFF9E9E9E)
    val UnansweredDark = Color(0xFF6B6B73)
}
