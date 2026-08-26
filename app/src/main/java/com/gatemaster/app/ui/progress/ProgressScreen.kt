package com.gatemaster.app.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.core.data.db.ScorePoint
import com.gatemaster.app.ui.AppViewModelProvider
import com.gatemaster.app.ui.components.EmptyState
import com.gatemaster.app.ui.theme.subjectAccent
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Progress", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            !state.hasHistory -> EmptyState(
                title = "Nothing to report yet",
                body = "Sit a practice test and this fills in: how your score is " +
                    "moving, which subjects are costing you marks, and the topics " +
                    "worth going back to.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Totals(state) }

                if (state.hasTrend) {
                    item { Heading("Score trend", "Percentage of the marks available") }
                    item { TrendChart(state.trend) }
                }

                if (state.subjects.isNotEmpty()) {
                    item {
                        Heading(
                            "By subject",
                            "Accuracy on the questions you actually answered, weakest first",
                        )
                    }
                    items(state.subjects, key = { it.subjectId }) { SubjectBar(it) }
                }

                if (state.weakTopics.isNotEmpty()) {
                    item {
                        Heading(
                            "Worth going back to",
                            "Topics you have answered at least three questions on",
                        )
                    }
                    items(state.weakTopics, key = { it.topicId }) { TopicRow(it) }
                }
            }
        }
    }
}

@Composable
private fun Totals(state: ProgressUiState, modifier: Modifier = Modifier) {
    val totals = state.totals
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Stat("${totals.attempts}", if (totals.attempts == 1) "test" else "tests")
                Stat("${(totals.accuracy * 100).roundToInt()}%", "accuracy")
                Stat(formatSpan(totals.timeSpentMs), "practising")
            }
            Text(
                text = "${totals.correct} right and ${totals.incorrect} wrong out of " +
                    "${totals.attemptedQuestions} answered" +
                    if (totals.unattempted > 0) ", ${totals.unattempted} left blank" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Heading(title: String, caption: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 10.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The score trend.
 *
 * Drawn against a fixed 0..100% axis rather than scaled to the data: a run of
 * poor scores should look poor, and auto-scaling would flatter it into a
 * healthy-looking line.
 */
@Composable
private fun TrendChart(points: List<ScorePoint>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val fill = line.copy(alpha = 0.18f)
    val grid = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(18.dp)) {
            Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                val w = size.width
                val h = size.height
                val step = if (points.size > 1) w / (points.size - 1) else w

                // Quarter gridlines, so a point can be read without a y axis.
                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                    val y = h - h * fraction
                    drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }

                val coords = points.mapIndexed { index, point ->
                    Offset(index * step, h - h * point.percent.coerceIn(0f, 1f))
                }

                val area = Path().apply {
                    moveTo(0f, h)
                    coords.forEach { lineTo(it.x, it.y) }
                    lineTo(coords.last().x, h)
                    close()
                }
                drawPath(area, fill)

                val stroke = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    coords.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(stroke, line, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

                coords.forEach { drawCircle(line, radius = 6f, center = it) }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${points.size} attempts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "latest ${(points.last().percent * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SubjectBar(subject: SubjectProgress, modifier: Modifier = Modifier) {
    val accent = subjectAccent(subject.subjectId)
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(subject.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${(subject.accuracy * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
        Bar(fraction = subject.accuracy, colour = accent, modifier = Modifier.padding(top = 6.dp))
        Text(
            "${subject.correct} of ${subject.attempted} answered correctly",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TopicRow(topic: TopicProgress, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${(topic.accuracy * 100).roundToInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(topic.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    buildString {
                        topic.subjectName?.let { append(it).append(" · ") }
                        append("${topic.correct} of ${topic.attempted} right")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, colour: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colour.copy(alpha = 0.18f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colour),
        )
    }
}

private fun formatSpan(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0L -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}
