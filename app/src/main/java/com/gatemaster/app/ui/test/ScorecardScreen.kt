package com.gatemaster.app.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gatemaster.app.core.model.QuestionResult
import com.gatemaster.app.core.model.QuestionType
import com.gatemaster.app.core.model.ResultKind
import com.gatemaster.app.core.model.Scorecard
import com.gatemaster.app.core.model.formatMarks
import com.gatemaster.app.ui.components.HtmlText
import com.gatemaster.app.ui.theme.LocalAnswerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(
    scorecard: Scorecard,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Result") }) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScoreHeadline(scorecard) }
            item { BreakdownCard(scorecard) }

            if (scorecard.sections.size > 1) {
                item {
                    Text("By section", style = MaterialTheme.typography.titleMedium)
                }
                items(scorecard.sections, key = { it.name }) { section ->
                    SectionRow(
                        name = section.name,
                        score = section.score,
                        maxMarks = section.maxMarks,
                    )
                }
            }

            item {
                Text(
                    text = "Review all ${scorecard.results.size} questions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(scorecard.results, key = { it.question.id }) { result ->
                ReviewRow(result)
            }
        }
    }
}

@Composable
private fun ScoreHeadline(scorecard: Scorecard, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${scorecard.scoreDisplay} / ${scorecard.maxMarks}",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "%.0f%% · %s".format(
                    scorecard.percentage.coerceAtLeast(0f),
                    formatDuration(scorecard.timeTakenMs),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (scorecard.marksLost > 0) {
                Text(
                    text = "−${formatMarks(scorecard.marksLost)} lost to negative marking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun BreakdownCard(scorecard: Scorecard, modifier: Modifier = Modifier) {
    val answers = LocalAnswerColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat("Correct", scorecard.correct, answers.correct)
                Stat("Wrong", scorecard.incorrect, answers.incorrect)
                Stat("Skipped", scorecard.unattempted, answers.unanswered)
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Accuracy on attempted questions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { scorecard.accuracy },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = if (scorecard.attempted == 0) {
                        "Nothing attempted"
                    } else {
                        "%.0f%% — %d right out of %d attempted".format(
                            scorecard.accuracy * 100,
                            scorecard.correct,
                            scorecard.attempted,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionRow(
    name: String,
    score: Double,
    maxMarks: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${formatMarks(score)} / $maxMarks",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReviewRow(result: QuestionResult, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val answers = LocalAnswerColors.current

    val (accent, verdict) = when (result.kind) {
        ResultKind.CORRECT -> answers.correct to "Correct"
        ResultKind.INCORRECT -> answers.incorrect to "Wrong"
        ResultKind.UNATTEMPTED -> answers.unanswered to "Skipped"
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent),
                )
                Text(
                    text = "  Q${result.question.number} · $verdict",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = signedMarks(result.marksAwarded),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HtmlText(
                html = result.question.text,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (expanded) {
                HorizontalDivider()
                AnswerSummary(result)
                result.question.solution?.takeIf { it.isNotBlank() }?.let { solution ->
                    Text(
                        text = "Solution",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HtmlText(html = solution, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AnswerSummary(result: QuestionResult, modifier: Modifier = Modifier) {
    val question = result.question
    val answers = LocalAnswerColors.current

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (question.type == QuestionType.NAT) {
            val yours = result.answer.numericInput.ifBlank { "—" }
            Text(
                text = "Your answer: $yours",
                style = MaterialTheme.typography.bodyMedium,
                color = if (result.isCorrect) answers.correct else answers.incorrect,
            )
            Text(
                text = "Accepted: ${question.numericAnswer?.display ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
                color = answers.correct,
            )
        } else {
            question.options.forEach { option ->
                val isCorrect = option.id in question.correctOptionIds
                val isChosen = option.id in result.answer.selectedOptionIds
                val tint = when {
                    isCorrect -> answers.correct
                    isChosen -> answers.incorrect
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val suffix = when {
                    isCorrect && isChosen -> "  ✓ your answer"
                    isCorrect -> "  ✓ correct answer"
                    isChosen -> "  ✗ your answer"
                    else -> ""
                }
                Row {
                    Text(
                        text = "${option.id}. ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tint,
                        fontWeight = if (isCorrect || isChosen) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    HtmlText(
                        html = option.text + suffix,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tint,
                    )
                }
            }
        }
    }
}

private fun signedMarks(value: Double): String = when {
    value > 0 -> "+${formatMarks(value)}"
    value < 0 -> formatMarks(value)
    else -> "0"
}

internal fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "%dh %02dm".format(hours, minutes)
        minutes > 0 -> "%dm %02ds".format(minutes, seconds)
        else -> "%ds".format(seconds)
    }
}
