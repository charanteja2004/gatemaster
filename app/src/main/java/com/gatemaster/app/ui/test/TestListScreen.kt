package com.gatemaster.app.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import com.gatemaster.app.core.model.AdaptivePlan
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.core.model.formatMarks
import com.gatemaster.app.ui.AppViewModelProvider
import com.gatemaster.app.ui.components.EmptyState
import com.gatemaster.app.ui.theme.subjectAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestListScreen(
    onBack: () -> Unit,
    onStartTest: (testId: String, restart: Boolean) -> Unit,
    onPractise: (subjectId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TestListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The counts and the resume badges are read once per load, and the thing
    // that changes them -- sitting a paper -- happens on another screen. Room
    // pushes what it can as a Flow; this covers the rest, which comes from the
    // assets and the in-progress attempt files.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Tests", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.tests.isEmpty() && state.practice.isEmpty() -> EmptyState(
                title = "No tests yet",
                body = "Practice sets appear here as questions are added for your paper.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // The recommendation leads, once there is a history to base one
                // on. Everything else on this screen asks the user what to
                // practise; this is the only thing that answers it.
                if (state.canRecommend) {
                    item {
                        SectionHeading(
                            title = "Recommended",
                            caption = "Drawn from the topics you get wrong and the ones " +
                                "you have not seen in a while",
                        )
                    }
                    item {
                        MixCard(
                            title = "Practise what needs it",
                            body = "${AdaptivePlan.QUESTION_COUNT} questions chosen from " +
                                "your ${state.practisedTopics} practised topics, " +
                                "weighted by the marks each subject carries",
                            icon = Icons.Filled.AutoAwesome,
                            onClick = { onStartTest(state.recommendedId, true) },
                        )
                    }
                }

                // Mixed papers lead the rest. A single-subject set tells you
                // how well you know that subject; only a mixed one tells you
                // which subject to spend tomorrow on.
                if (state.canMix) {
                    item {
                        SectionHeading(
                            title = "Mixed tests",
                            caption = "Several subjects in one paper, scored subject by subject",
                        )
                    }
                    item {
                        MixCard(
                            title = "Everything",
                            body = "30 questions drawn across all ${state.mixSubjects.size} " +
                                "subjects with a question bank",
                            icon = Icons.Filled.Shuffle,
                            onClick = { onStartTest(state.everythingMixId, false) },
                        )
                    }
                    item {
                        MixCard(
                            title = "Choose subjects",
                            body = "Build a paper from just the subjects you are revising",
                            icon = Icons.Filled.Tune,
                            onClick = viewModel::chooseMix,
                        )
                    }
                }

                if (state.practice.isNotEmpty()) {
                    item {
                        SectionHeading(
                            title = "Subject practice",
                            caption = "One subject at a time, twenty questions",
                        )
                    }
                    items(state.practice, key = { it.subjectId }) { entry ->
                        PracticeCard(entry = entry, onClick = { onPractise(entry.subjectId) })
                    }
                }

                if (state.tests.isNotEmpty()) {
                    item {
                        SectionHeading(
                            title = "Full tests",
                            caption = "Timed, exam length",
                        )
                    }
                }

                items(state.tests, key = { it.summary.id }) { entry ->
                    TestCard(
                        title = entry.summary.title,
                        description = entry.summary.description,
                        meta = "${entry.summary.questionCount} questions · " +
                            "${entry.summary.totalMarks} marks · " +
                            "${entry.summary.durationMinutes} min",
                        inProgress = entry.inProgress,
                        onClick = {
                            if (entry.inProgress) {
                                viewModel.askResumeOrRestart(entry.summary.id)
                            } else {
                                onStartTest(entry.summary.id, false)
                            }
                        },
                    )
                }

                if (state.history.isNotEmpty()) {
                    item {
                        SectionHeading(title = "Past attempts", caption = null)
                    }
                    items(state.history, key = { it.id }) { record ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(record.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${record.correct} right · ${record.incorrect} wrong · " +
                                        formatDuration(record.timeTakenMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${formatMarks(record.score)}/${record.maxMarks}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.isChoosingMix) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissMixPicker) {
            MixPicker(
                subjects = state.mixSubjects,
                selected = state.selectedMix,
                onToggle = viewModel::toggleMixSubject,
                startId = state.customMixId,
                onStart = { testId ->
                    viewModel.dismissMixPicker()
                    onStartTest(testId, false)
                },
            )
        }
    }

    state.resumePromptTestId?.let { testId ->
        AlertDialog(
            onDismissRequest = viewModel::dismissResumePrompt,
            title = { Text("Continue where you left off?") },
            text = { Text("You have an unfinished attempt at this test.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissResumePrompt()
                    onStartTest(testId, false)
                }) { Text("Resume") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissResumePrompt()
                    onStartTest(testId, true)
                }) { Text("Start over") }
            },
        )
    }
}

@Composable
private fun SectionHeading(
    title: String,
    caption: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(top = 10.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** An entry point to a multi-subject paper: the whole lot, or a chosen few. */
@Composable
private fun MixCard(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                )
            }
        }
    }
}

/**
 * Subject picker for a custom mix.
 *
 * The start button carries the question count rather than sitting there as a
 * bare "Start", because the number is the thing that changes as subjects are
 * ticked, and a paper of six questions is worth knowing about before starting.
 */
@Composable
private fun MixPicker(
    subjects: List<PracticeEntry>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    startId: String?,
    onStart: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Choose subjects", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "The paper is drawn evenly from whatever you pick, and scored " +
                "one section per subject.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        subjects.forEach { entry ->
            val isSelected = entry.subjectId in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggle(entry.subjectId) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggle(entry.subjectId) })
                Column(Modifier.weight(1f)) {
                    Text(entry.subjectName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${entry.questionCount} questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = { startId?.let(onStart) },
            enabled = startId != null,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Text(
                if (startId == null) {
                    "Pick at least two subjects"
                } else {
                    "Start mixed test"
                },
            )
        }
    }
}

/**
 * One subject's practice set. Tapping it builds a paper from that subject's
 * questions; the per-topic sets live on the topic rows inside the subject.
 */
@Composable
private fun PracticeCard(
    entry: PracticeEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = subjectAccent(entry.subjectId)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = accent.copy(alpha = 0.10f),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(entry.subjectName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = buildString {
                        append("${entry.questionCount} questions")
                        // Only mention topic sets when there are any, and get
                        // the plural right -- "1 topics" undoes a lot of polish.
                        when (entry.topicCount) {
                            0 -> Unit
                            1 -> append(" · 1 topic also has its own set")
                            else -> append(" · ${entry.topicCount} topics also have their own sets")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TestCard(
    title: String,
    description: String,
    meta: String,
    inProgress: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (description.isNotBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (inProgress) {
                Text(
                    text = "In progress",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
