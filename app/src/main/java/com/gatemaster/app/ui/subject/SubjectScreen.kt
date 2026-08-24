package com.gatemaster.app.ui.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.core.model.ContentRef
import com.gatemaster.app.core.model.ContentType
import com.gatemaster.app.ui.AppViewModelProvider
import com.gatemaster.app.ui.components.EmptyState
import com.gatemaster.app.ui.theme.subjectAccent

/** What the reader needs to open a document. */
data class OpenRequest(
    val title: String,
    val subtitle: String,
    val ref: ContentRef,
    /** Set for topics, so the reader can offer previous/next. */
    val subjectId: String? = null,
    val topicId: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectScreen(
    onBack: () -> Unit,
    onOpen: (OpenRequest) -> Unit,
    onPractise: (subjectId: String, topicId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubjectViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subject = state.subject
    val accent = subjectAccent(subject?.id.orEmpty())

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = subject?.name ?: "Subject",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                        subject?.let {
                            Text(
                                text = if (state.readCount > 0) {
                                    "${state.readCount} of ${it.topics.size} read"
                                } else {
                                    "~${it.weightage} marks in the paper"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.canPractiseSubject && subject != null) {
                        TextButton(onClick = { onPractise(subject.id, null) }) {
                            Icon(
                                Icons.Filled.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Text(" Practise")
                        }
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
                body = "This subject is not part of the paper you have selected.",
                modifier = Modifier.padding(padding),
            )

            subject.isEmpty -> EmptyState(
                title = subject.name,
                body = "This section is worth about ${subject.weightage} marks in the paper. " +
                    "The detailed syllabus and notes for it are still being written — " +
                    "General Aptitude, which is 15 marks of every paper, is ready now.",
                modifier = Modifier.padding(padding),
            )

            else -> Column(Modifier.padding(padding)) {
                val tabs = state.availableTabs
                if (tabs.size > 1) {
                    SecondaryScrollableTabRow(
                        selectedTabIndex = tabs.indexOf(state.selectedTab).coerceAtLeast(0),
                        edgePadding = 12.dp,
                    ) {
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
                                accent = accent,
                                isRead = state.isRead(topic.id),
                                isBookmarked = state.isBookmarked(topic.id),
                                onPractise = if (topic.id in state.practisableTopics) {
                                    { onPractise(subject.id, topic.id) }
                                } else {
                                    null
                                },
                                onClick = {
                                    onOpen(
                                        OpenRequest(
                                            title = topic.title,
                                            subtitle = subject.name,
                                            ref = topic.content,
                                            subjectId = subject.id,
                                            topicId = topic.id,
                                        ),
                                    )
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
                                accent = accent,
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
                                    accent = accent,
                                    onClick = {
                                        onOpen(OpenRequest("Short notes", subject.name, ref))
                                    },
                                )
                            }
                        }

                        SubjectTab.SYLLABUS -> {
                            item {
                                Text(
                                    text = "Official GATE syllabus for this subject.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            itemsIndexed(subject.syllabus) { index, line ->
                                SyllabusRow(index = index + 1, text = line, accent = accent)
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
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRead: Boolean = false,
    isBookmarked: Boolean = false,
    onPractise: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPdf) {
                        Icons.Filled.PictureAsPdf
                    } else {
                        Icons.AutoMirrored.Filled.Article
                    },
                    contentDescription = if (isPdf) "PDF" else "Article",
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    // Read topics recede so unread ones stand out in a long list.
                    color = if (isRead) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = if (isRead) "Read" else caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRead) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isBookmarked) {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Bookmarked",
                    modifier = Modifier.size(16.dp),
                    tint = accent,
                )
            }
            if (isRead) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Read",
                    modifier = Modifier.size(18.dp),
                    tint = accent,
                )
            }

            // A ten-question set for this topic alone. This is the point of the
            // whole feature: a study session that fits in a spare five minutes.
            if (onPractise != null) {
                IconButton(onClick = onPractise) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = "Practise this topic",
                        modifier = Modifier.size(19.dp),
                        tint = accent,
                    )
                }
            }
        }
    }
}

/**
 * The syllabus is the one thing every candidate looks up, and it is available
 * for all 30 papers even where no notes exist yet.
 */
@Composable
private fun SyllabusRow(
    index: Int,
    text: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "PDF"
    bytes < 1024 * 1024 -> "PDF · ${bytes / 1024} KB"
    else -> "PDF · %.1f MB".format(bytes / (1024.0 * 1024.0))
}
