package com.gatemaster.app.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Blurs whatever is behind the composable.
 *
 * Real backdrop blur needs [RenderEffect], which arrived in Android 12. Below
 * that there is no way to sample the backdrop, so the glass panels fall back to
 * a translucent tint — which is why every use pairs the blur with a visible
 * fill rather than relying on the blur alone to make text legible.
 */
fun Modifier.backdropBlur(radius: Dp): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.then(
            Modifier.graphicsLayer {
                renderEffect = RenderEffect
                    .createBlurEffect(
                        radius.toPx(),
                        radius.toPx(),
                        Shader.TileMode.CLAMP,
                    )
                    .asComposeRenderEffect()
            },
        )
    } else {
        this
    }

/**
 * A frosted panel: translucent fill, a bright hairline along the top edge, and
 * a soft border.
 *
 * The hairline is what actually reads as glass — it mimics light catching the
 * lip of a pane, and without it a translucent box just looks faded.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tint: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.62f,
    content: @Composable BoxScope.() -> Unit,
) {
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        tint.copy(alpha = alpha + 0.10f),
                        tint.copy(alpha = alpha - 0.06f),
                    ),
                ),
            )
            .border(1.dp, outline, shape),
        content = content,
    )
}

/**
 * Highlight for the top edge of a glass panel, drawn as its own gradient so it
 * fades out rather than ending abruptly at the corners.
 */
@Composable
fun glassSheen(): Brush = Brush.horizontalGradient(
    0f to Color.Transparent,
    0.5f to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
    1f to Color.Transparent,
)
