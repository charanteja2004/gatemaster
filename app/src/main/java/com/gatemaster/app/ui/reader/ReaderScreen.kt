package com.gatemaster.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.core.model.Topic
import com.gatemaster.app.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    title: String,
    subtitle: String,
    assetPath: String,
    isPdf: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onOpenTopic: (Topic) -> Unit = {},
    viewModel: ReaderViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTextControls by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = state.progress,
        label = "reading-progress",
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    actions = {
                        if (!isPdf) {
                            IconButton(onClick = { showTextControls = !showTextControls }) {
                                Icon(
                                    Icons.Filled.FormatSize,
                                    contentDescription = "Text size",
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )

                AnimatedVisibility(
                    visible = showTextControls,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    TextSizeBar(
                        zoom = state.textZoom,
                        canShrink = state.canShrink,
                        canGrow = state.canGrow,
                        onShrink = viewModel::shrink,
                        onGrow = viewModel::grow,
                    )
                }

                // How much of the article is left — a long page stops feeling
                // bottomless when you can see the end approaching.
                if (!isPdf) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        },
        bottomBar = {
            if (!isPdf && (state.previous != null || state.next != null)) {
                TopicPager(
                    previous = state.previous,
                    next = state.next,
                    onOpenTopic = onOpenTopic,
                )
            }
        },
    ) { padding ->
        if (isPdf) {
            PdfReader(assetPath = assetPath, modifier = Modifier.padding(padding))
        } else {
            HtmlReader(
                assetPath = assetPath,
                textZoom = state.textZoom,
                isDarkTheme = isDarkTheme,
                onProgress = viewModel::onProgress,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun TextSizeBar(
    zoom: Int,
    canShrink: Boolean,
    canGrow: Boolean,
    onShrink: () -> Unit,
    onGrow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Text size",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onShrink, enabled = canShrink) {
                Text("A", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "$zoom%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onGrow, enabled = canGrow) {
                Text("A", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/**
 * Moving to the next topic without going back to the list is what turns a pile
 * of articles into a study session.
 */
@Composable
private fun TopicPager(
    previous: Topic?,
    next: Topic?,
    onOpenTopic: (Topic) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PagerButton(
                topic = previous,
                label = "Previous",
                forward = false,
                onClick = onOpenTopic,
                modifier = Modifier.weight(1f),
            )
            PagerButton(
                topic = next,
                label = "Next",
                forward = true,
                onClick = onOpenTopic,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PagerButton(
    topic: Topic?,
    label: String,
    forward: Boolean,
    onClick: (Topic) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (topic == null) {
        Box(modifier)
        return
    }

    TextButton(
        onClick = { onClick(topic) },
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!forward) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = if (forward) Alignment.End else Alignment.Start,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (forward) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
