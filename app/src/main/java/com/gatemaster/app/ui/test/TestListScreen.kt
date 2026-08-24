package com.gatemaster.app.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.core.model.formatMarks
import com.gatemaster.app.ui.AppViewModelProvider
import com.gatemaster.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestListScreen(
    onBack: () -> Unit,
    onStartTest: (testId: String, restart: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TestListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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

            state.tests.isEmpty() -> EmptyState(
                title = "No tests yet",
                body = "Mock tests will appear here as they are added.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                        Text(
                            "Past attempts",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    items(state.history, key = { it.submittedAtEpochMs }) { record ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(record.testTitle, style = MaterialTheme.typography.bodyMedium)
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
