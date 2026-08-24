package com.gatemaster.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.core.model.Subject
import com.gatemaster.app.ui.AppViewModelProvider
import com.gatemaster.app.ui.components.EmptyState
import com.gatemaster.app.ui.theme.subjectAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSubjectClick: (String) -> Unit,
    onPapersClick: () -> Unit,
    onTestsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onChangeBranch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("GateMaster", fontWeight = FontWeight.Bold) },
                actions = {
                    state.branch?.let { branch ->
                        BranchChip(code = branch.code, onClick = onChangeBranch)
                        Spacer(Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.errorMessage != null -> EmptyState(
                title = "Could not open your study material",
                body = state.errorMessage.orEmpty(),
                modifier = Modifier.padding(padding),
                action = { TextButton(onClick = viewModel::retry) { Text("Try again") } },
            )

            else -> HomeContent(
                state = state,
                onSubjectClick = onSubjectClick,
                onPapersClick = onPapersClick,
                onTestsClick = onTestsClick,
                onSearchClick = onSearchClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onSubjectClick: (String) -> Unit,
    onPapersClick: () -> Unit,
    onTestsClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { CountdownHero(state) }
        item { SearchEntry(onClick = onSearchClick) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile(
                    icon = Icons.Filled.EditNote,
                    title = "Mock tests",
                    caption = "Timed, with scorecard",
                    onClick = onTestsClick,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    icon = Icons.Filled.Description,
                    title = "Past papers",
                    caption = if (state.paperCount > 0) {
                        "${state.paperCount} papers"
                    } else {
                        "Coming for ${state.branch?.code.orEmpty()}"
                    },
                    onClick = onPapersClick,
                    enabled = state.paperCount > 0,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("Subjects", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${state.subjectsWithNotes} of ${state.subjects.size} with notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(state.subjects, key = { it.id }) { subject ->
            SubjectRow(subject = subject, onClick = { onSubjectClick(subject.id) })
        }
    }
}

/**
 * The focal point of the home screen: how long is left, and what the user is
 * preparing for. A countdown is the one number every aspirant already carries
 * in their head.
 */
@Composable
private fun CountdownHero(state: HomeUiState, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(scheme.primaryContainer, scheme.tertiaryContainer),
                ),
            ),
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = state.branch?.name.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${state.daysToExam}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onPrimaryContainer,
                    )
                    Text(
                        text = "  days to GATE ${state.examYear}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Text(
                    text = "${state.totalItems} items across ${state.subjects.size} subjects",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun BranchChip(code: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Icon(
                Icons.Filled.SwapHoriz,
                contentDescription = "Change paper",
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Search topics",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SubjectRow(
    subject: Subject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = subjectAccent(subject.id)
    val weightFraction by animateFloatAsState(
        targetValue = (subject.weightage / 25f).coerceIn(0.05f, 1f),
        label = "weightage",
    )

    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Colour rail: gives the subject an identity and makes a long list
            // scannable without adding another line of text.
            Box(
                Modifier
                    .width(5.dp)
                    .height(78.dp)
                    .background(accent),
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 12.dp, top = 13.dp, bottom = 13.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "~${subject.weightage}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    Text(
                        text = " marks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(weightFraction)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent),
                    )
                }

                Text(
                    text = if (subject.isSyllabusOnly) {
                        "Syllabus · ${subject.syllabus.size} areas"
                    } else {
                        "${subject.noteCount} items · syllabus"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp).size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
