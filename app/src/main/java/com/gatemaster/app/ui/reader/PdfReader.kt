package com.gatemaster.app.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gatemaster.app.ui.components.EmptyState

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 3f

/**
 * Vertical page-by-page PDF viewer with pinch-to-zoom.
 *
 * Pages render on demand at the current zoom width, so a 40-page paper does not
 * decode 40 bitmaps up front.
 */
@Composable
fun PdfReader(
    assetPath: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val documentResult by produceState<Result<PdfDocument>?>(initialValue = null, assetPath) {
        value = PdfDocument.openFromAsset(context, assetPath)
    }

    // Close the renderer when we navigate away or switch documents.
    DisposableEffect(documentResult) {
        onDispose { documentResult?.getOrNull()?.close() }
    }

    when {
        documentResult == null -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        documentResult?.isFailure == true -> EmptyState(
            title = "This document could not be opened",
            body = "The file may be damaged. Reinstalling the app will restore it.",
            modifier = modifier,
        )

        else -> PdfPages(
            document = documentResult!!.getOrThrow(),
            modifier = modifier,
        )
    }
}

@Composable
private fun PdfPages(
    document: PdfDocument,
    modifier: Modifier = Modifier,
) {
    var zoom by remember { mutableFloatStateOf(MIN_ZOOM) }
    // Signature is (centroid, zoomChange, panChange, rotationChange).
    val transformState = rememberTransformableState { _, zoomChange, _, _ ->
        zoom = (zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        val containerWidth = maxWidth
        val density = LocalDensity.current
        val pageWidth = containerWidth * zoom
        val renderWidthPx = with(density) { pageWidth.roundToPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .transformable(transformState),
        ) {
            LazyColumn(
                modifier = Modifier.width(pageWidth),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    count = document.pageCount,
                    key = { it },
                ) { index ->
                    PdfPage(
                        document = document,
                        index = index,
                        renderWidthPx = renderWidthPx,
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfPage(
    document: PdfDocument,
    index: Int,
    renderWidthPx: Int,
    modifier: Modifier = Modifier,
) {
    val size = document.pageSizes.getOrNull(index)
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        index,
        renderWidthPx,
    ) {
        value = document.renderPage(index, renderWidthPx)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        color = androidx.compose.ui.graphics.Color.White,
        shadowElevation = 1.dp,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Page ${index + 1} of ${document.pageCount}",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(size?.aspectRatio ?: 0.707f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
