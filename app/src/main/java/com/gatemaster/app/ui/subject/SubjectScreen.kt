package com.gatemaster.app.ui.subject

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.core.model.ContentRef
import com.gatemaster.app.core.model.ContentType
import com.gatemaster.app.ui.AppViewModelProvider
import com.gatemaster.app.ui.components.EmptyState

/** What the reader needs to open a document. */
data class OpenRequest(
    val title: String,
    val subtitle: String,
    val ref: ContentRef,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectScreen(
    onBack: () -> Unit,
    onOpen: (OpenRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubjectViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subject = state.subject

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(subject?.name ?: "Subject") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            subject == null -> EmptyState(
                title = "Subject not found",
                body = "This subject is not in the study material bundled with the app.",
                modifier = Modifier.padding(padding),
            )

            subject.isEmpty -> EmptyState(
                title = "${subject.name} is not ready yet",
                body = "This subject is on the roadmap. It is worth about " +
                    "${subject.weightage} marks in the GATE paper, so it is a priority.",
                modifier = Modifier.padding(padding),
            )

            else -> Column(Modifier.padding(padding)) {
                val tabs = state.availableTabs
                if (tabs.size > 1) {
                    SecondaryTabRow(selectedTabIndex = tabs.indexOf(state.selectedTab).coerceAtLeast(0)) {
                        tabs.forEach { tab ->
                            Tab(
                                selected = tab == state.selectedTab,
                                onClick = { viewModel.selectTab(tab) },
                                text = { Text(tab.label) },
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (state.selectedTab) {
                        SubjectTab.TOPICS -> items(subject.topics, key = { it.id }) { topic ->
                            DocumentRow(
                                title = topic.title,
                                caption = "Notes",
                                isPdf = topic.content.type == ContentType.PDF,
                                onClick = {
                                    onOpen(OpenRequest(topic.title, subject.name, topic.content))
                                },
                            )
                        }

                        SubjectTab.HANDOUTS -> items(
                            subject.referenceNotes,
                            key = { it.id },
                        ) { note ->
                            DocumentRow(
                                title = note.title,
                                caption = formatSize(note.sizeBytes),
                                isPdf = true,
                                onClick = {
                                    onOpen(OpenRequest(note.title, subject.name, note.content))
                                },
                            )
                        }

                        SubjectTab.REVISION -> subject.shortNotes?.let { ref ->
                            item {
                                DocumentRow(
                                    title = "${subject.name} — short notes",
                                    caption = "Quick revision",
                                    isPdf = ref.type == ContentType.PDF,
                                    onClick = {
                                        onOpen(
                                            OpenRequest(
                                                "Short notes",
                                                subject.name,
                                                ref,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(
    title: String,
    caption: String,
    isPdf: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = if (isPdf) Icons.Filled.PictureAsPdf else Icons.AutoMirrored.Filled.Article,
                contentDescription = if (isPdf) "PDF" else "Article",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "PDF"
    bytes < 1024 * 1024 -> "PDF · ${bytes / 1024} KB"
    else -> "PDF · %.1f MB".format(bytes / (1024.0 * 1024.0))
}
