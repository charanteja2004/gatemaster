package com.gatemaster.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.ui.AppViewModelProvider
import com.gatemaster.app.ui.components.EmptyState
import com.gatemaster.app.ui.subject.SubjectCard

/**
 * The hero keeps one fixed identity in both themes.
 *
 * Deriving it from the colour scheme made it invert: in dark mode
 * `colorScheme.primary` is a pale indigo, so the panel turned into a bright
 * slab on an otherwise dark page — glary at night and visually detached from
 * everything below it. A deep gradient with light text reads correctly on
 * either ground.
 */
private val HeroGradient = listOf(
    Color(0xFF243A8F),
    Color(0xFF3B3FA8),
    Color(0xFF6A3E9C),
)

private val HeroInk = Color(0xFFF3F4FF)

@Composable
fun HomeScreen(
    onSubjectClick: (String) -> Unit,
    onPapersClick: () -> Unit,
    onTestsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onChangeBranch: () -> Unit,
    onSeeAllSubjects: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier) { padding ->
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

            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                item {
                    Hero(
                        branchName = state.branch?.name.orEmpty(),
                        branchCode = state.branch?.code.orEmpty(),
                        days = state.daysToExam,
                        examYear = state.examYear,
                        onChangeBranch = onChangeBranch,
                    )
                }

                item { SearchPill(onClick = onSearchClick) }

                item {
                    // Two-by-two stat grid: the numbers a candidate checks
                    // rather than a paragraph they would skip.
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(
                                value = "${state.totalItems}",
                                label = "Study items",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                value = "${state.subjects.size}",
                                label = "Subjects",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Highest weightage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = onSeeAllSubjects) { Text("See all") }
                    }
                }

                // Home is a dashboard, not the whole syllabus: the four
                // heaviest subjects, then a way through to the rest.
                items(state.topSubjects, key = { it.id }) { subject ->
                    SubjectCard(
                        subject = subject,
                        onClick = { onSubjectClick(subject.id) },
                    )
                }
            }
        }
    }
}

/**
 * The focal point. A countdown is the one number every aspirant already carries
 * in their head, so the page opens with it.
 */
@Composable
private fun Hero(
    branchName: String,
    branchCode: String,
    days: Long,
    examYear: Int,
    onChangeBranch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
    ) {
        Box {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(HeroGradient)),
            )

            // Faint concentric rings, echoing the reference layouts. Drawn
            // rather than shipped as an image so it scales and themes freely.
            Canvas(Modifier.matchParentSize()) {
                val centre = Offset(size.width * 0.88f, size.height * 0.12f)
                repeat(4) { i ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.07f),
                        radius = size.minDimension * (0.28f + i * 0.20f),
                        center = centre,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f * density),
                    )
                }
            }

            Column(
                Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Preparing for",
                        style = MaterialTheme.typography.labelMedium,
                        color = HeroInk.copy(alpha = 0.82f),
                        modifier = Modifier.weight(1f),
                    )
                    BranchChip(code = branchCode, onClick = onChangeBranch)
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = branchName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HeroInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$days",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = HeroInk,
                    )
                    Text(
                        text = "  days to GATE $examYear",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HeroInk.copy(alpha = 0.88f),
                        modifier = Modifier.padding(bottom = 7.dp),
                    )
                }
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
        color = HeroInk.copy(alpha = 0.18f),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 9.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = HeroInk,
            )
            Icon(
                Icons.Filled.SwapHoriz,
                contentDescription = "Change paper",
                modifier = Modifier.size(16.dp),
                tint = HeroInk,
            )
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Search topics",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = tint.copy(alpha = 0.12f),
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
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
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (enabled) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
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
