package com.gatemaster.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * A stable accent colour per subject.
 *
 * A list of thirty identically grey rows is unreadable at a glance; giving each
 * subject its own hue makes the list scannable and gives a subject the same
 * identity everywhere it appears. Well-known subjects are pinned so that, say,
 * Operating Systems is always the same colour; anything unpinned derives a hue
 * from its id, so new subjects and other branches still get a stable colour
 * without anyone maintaining a table.
 *
 * Both variants are tuned to sit on their theme's surface with legible
 * contrast — these are not the same colour dimmed.
 */
private data class Accent(val light: Color, val dark: Color)

private val PINNED: Map<String, Accent> = mapOf(
    "aptitude" to Accent(Color(0xFF00696E), Color(0xFF4EDBE3)),   // teal
    "maths" to Accent(Color(0xFF6B4BA8), Color(0xFFCFB4FF)),      // violet
    "ds" to Accent(Color(0xFF1B6C3A), Color(0xFF6FDD97)),         // green
    "os" to Accent(Color(0xFFA23F1E), Color(0xFFFFB59B)),         // burnt orange
    "algo" to Accent(Color(0xFF2B4BB8), Color(0xFFB8C3FF)),       // indigo
    "dbms" to Accent(Color(0xFF8A5100), Color(0xFFFFB868)),       // amber
    "cao" to Accent(Color(0xFF7A2E5B), Color(0xFFFFAEDC)),        // magenta
    "toc" to Accent(Color(0xFF00629E), Color(0xFF8ECDFF)),        // azure
    "cn" to Accent(Color(0xFF00695C), Color(0xFF70D9C6)),         // sea green
    "dl" to Accent(Color(0xFF8C3A3A), Color(0xFFFFB3B0)),         // brick
    "cd" to Accent(Color(0xFF4E5B25), Color(0xFFC9D98D)),         // olive
)

/**
 * Hues chosen to be distinguishable from one another and from the pinned set.
 * Each pair is hand-balanced rather than generated, so no entry comes out muddy
 * on either ground.
 */
private val FALLBACK: List<Accent> = listOf(
    Accent(Color(0xFF1D5FA8), Color(0xFF9FC9FF)),
    Accent(Color(0xFF2E6B4F), Color(0xFF8FD9B6)),
    Accent(Color(0xFF8E4B10), Color(0xFFFFC08A)),
    Accent(Color(0xFF6A3E9C), Color(0xFFD5BBFF)),
    Accent(Color(0xFF8A3C55), Color(0xFFFFB1C6)),
    Accent(Color(0xFF0F6570), Color(0xFF7FD8E4)),
    Accent(Color(0xFF5B5A1C), Color(0xFFDCD98A)),
    Accent(Color(0xFF9B3A22), Color(0xFFFFB4A0)),
    Accent(Color(0xFF334C9E), Color(0xFFB4C2FF)),
    Accent(Color(0xFF2F6330), Color(0xFF9CD69C)),
)

@Composable
@ReadOnlyComposable
fun subjectAccent(subjectId: String): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accent = PINNED[subjectId]
        ?: FALLBACK[abs(subjectId.hashCode()) % FALLBACK.size]
    return if (dark) accent.dark else accent.light
}

/** Relative luminance, used only to pick the light or dark variant. */
private fun Color.luminance(): Float =
    (0.2126f * red + 0.7152f * green + 0.0722f * blue)
