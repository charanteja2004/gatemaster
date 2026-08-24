package com.gatemaster.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Fades and lifts a list item into place, staggered by its position.
 *
 * The stagger is capped: past a handful of items the delay stops growing, so a
 * long list never leaves the reader waiting for row twenty to arrive.
 */
@Composable
fun Modifier.enterFromBelow(
    index: Int,
    stagger: Int = 45,
    maxStaggeredItems: Int = 8,
): Modifier {
    val progress = remember { Animatable(0f) }
    val offsetPx = with(LocalDensity.current) { 18.dp.toPx() }

    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(maxStaggeredItems) * stagger).toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
        )
    }

    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offsetPx
    }
}

/**
 * Shrinks slightly while held.
 *
 * A card that reacts under the finger feels responsive in a way that a ripple
 * alone does not, and it costs one animated float.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.972f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press-scale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Counts up to [target] once, on first appearance.
 *
 * Used for the days-to-exam figure: watching it land is a small moment of
 * drama on a screen that is otherwise a list of things still to do.
 */
@Composable
fun animatedCount(target: Long, durationMillis: Int = 900): State<Int> {
    val animated = remember { Animatable(0f) }

    LaunchedEffect(target) {
        if (target <= 0) {
            animated.snapTo(0f)
            return@LaunchedEffect
        }
        animated.snapTo(0f)
        animated.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis, easing = LinearOutSlowInEasing),
        )
    }

    return remember(animated) {
        derivedIntState { animated.value.toInt() }
    }
}

private fun derivedIntState(compute: () -> Int): State<Int> =
    androidx.compose.runtime.derivedStateOf(compute)
