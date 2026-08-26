package com.gatemaster.app.ui.test

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.core.model.Question
import com.gatemaster.app.core.model.QuestionStatus
import com.gatemaster.app.core.model.QuestionType
import com.gatemaster.app.ui.AppViewModelProvider
import com.gatemaster.app.ui.components.EmptyState
import com.gatemaster.app.ui.components.HtmlText
import com.gatemaster.app.ui.theme.LocalAnswerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestPlayerScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TestPlayerViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The clock runs only while the test is on screen, so a practice attempt
    // does not burn its timer in the background.
    LifecycleResumeEffect(Unit) {
        viewModel.resumeTimer()
        onPauseOrDispose { viewModel.pauseTimer() }
    }

    val scorecard = state.scorecard
    if (scorecard != null) {
        ScorecardScreen(scorecard = scorecard, onDone = onExit, modifier = modifier)
        return
    }

    // Leaving mid-test should be deliberate, not one stray back gesture.
    BackHandler(enabled = state.test != null) { viewModel.askToSubmit() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.test?.title.orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                        )
                        Text(
                            text = "Question ${state.currentIndex + 1} of ${state.questions.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.askToSubmit() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Leave test")
                    }
                },
                actions = {
                    CountdownChip(remainingMs = state.remainingMs)
                    IconButton(onClick = { viewModel.showPalette(true) }) {
                        Icon(Icons.Filled.GridView, contentDescription = "Question palette")
                    }
                },
            )
        },
        bottomBar = {
            if (state.test != null) {
                PlayerControls(
                    isFirst = state.isFirst,
                    isLast = state.isLast,
                    isMarked = state.currentAnswer?.status?.isMarked == true,
                    hasResponse = state.currentAnswer?.hasResponse == true,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onMark = viewModel::toggleMarkForReview,
                    onClear = viewModel::clearResponse,
                    onSubmit = viewModel::askToSubmit,
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.errorMessage != null -> EmptyState(
                title = "Test unavailable",
                body = state.errorMessage.orEmpty(),
                modifier = Modifier.padding(padding),
                action = { TextButton(onClick = onExit) { Text("Go back") } },
            )

            else -> {
                val question = state.currentQuestion
                if (question == null) {
                    EmptyState(
                        title = "This test has no questions",
                        body = "The question bank could not be read.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    QuestionPane(
                        question = question,
                        selectedOptionIds = state.currentAnswer?.selectedOptionIds.orEmpty(),
                        numericInput = state.currentAnswer?.numericInput.orEmpty(),
                        onSelectOption = viewModel::selectOption,
                        onNumericChange = viewModel::setNumericInput,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }

    if (state.showPalette) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.showPalette(false) },
            sheetState = sheetState,
        ) {
            QuestionPalette(
                questions = state.questions,
                sections = state.paletteSections,
                showHeadings = state.showsSectionHeadings,
                currentIndex = state.currentIndex,
                statusOf = state::statusOf,
                onSelect = viewModel::goTo,
            )
        }
    }

    if (state.showSubmitConfirm) {
        val unanswered = state.questions.size - state.answeredCount
        AlertDialog(
            onDismissRequest = viewModel::dismissSubmit,
            title = { Text("Submit this test?") },
            text = {
                Text(
                    buildString {
                        append("${state.answeredCount} answered")
                        if (unanswered > 0) append(", $unanswered left blank")
                        if (state.markedCount > 0) append(", ${state.markedCount} marked for review")
                        append(".\n\nYou will see your score and every solution straight away.")
                    },
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.submit() }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSubmit) { Text("Keep going") }
            },
        )
    }
}

@Composable
private fun CountdownChip(remainingMs: Long, modifier: Modifier = Modifier) {
    val totalSeconds = remainingMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val text = if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }

    val urgent = remainingMs in 1..(5 * 60_000L)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (urgent) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Filled.Timer,
            contentDescription = "Time remaining",
            modifier = Modifier.size(16.dp),
            tint = if (urgent) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (urgent) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun QuestionPane(
    question: Question,
    selectedOptionIds: Set<String>,
    numericInput: String,
    onSelectOption: (String) -> Unit,
    onNumericChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuestionMeta(question)

        HtmlText(
            html = question.text,
            style = MaterialTheme.typography.bodyLarge,
        )

        when (question.type) {
            QuestionType.MCQ, QuestionType.MSQ -> question.options.forEach { option ->
                OptionRow(
                    label = option.id,
                    text = option.text,
                    selected = option.id in selectedOptionIds,
                    multiSelect = question.type == QuestionType.MSQ,
                    onClick = { onSelectOption(option.id) },
                )
            }

            QuestionType.NAT -> OutlinedTextField(
                value = numericInput,
                onValueChange = onNumericChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your answer") },
                placeholder = { Text("e.g. 12.5") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("Numerical answer — no options, and no negative marking.") },
            )
        }
    }
}

@Composable
private fun QuestionMeta(question: Question, modifier: Modifier = Modifier) {
    val typeLabel = when (question.type) {
        QuestionType.MCQ -> "Single correct"
        QuestionType.MSQ -> "One or more correct"
        QuestionType.NAT -> "Numerical answer"
    }
    val marks = if (question.marks == 1) "1 mark" else "${question.marks} marks"
    val penalty = if (question.negativeMarks > 0) {
        " · −${com.gatemaster.app.core.model.formatMarks(question.negativeMarks)} if wrong"
    } else {
        " · no negative marking"
    }

    Text(
        text = "Q${question.number} · $typeLabel · $marks$penalty",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun OptionRow(
    label: String,
    text: String,
    selected: Boolean,
    multiSelect: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (multiSelect) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            } else {
                RadioButton(selected = selected, onClick = onClick)
            }
            Text(
                text = "$label.",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(end = 10.dp),
            )
            HtmlText(html = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PlayerControls(
    isFirst: Boolean,
    isLast: Boolean,
    isMarked: Boolean,
    hasResponse: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMark: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onMark) {
                    Icon(
                        if (isMarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (isMarked) " Marked" else " Mark for review",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (hasResponse) {
                    TextButton(onClick = onClear) {
                        Text("Clear", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = !isFirst,
                    modifier = Modifier.weight(1f),
                ) { Text("Previous") }

                if (isLast) {
                    Button(onClick = onSubmit, modifier = Modifier.weight(1f)) {
                        Text("Submit test")
                    }
                } else {
                    Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                        // "Save & Next" is the GATE wording, and it is accurate:
                        // the answer is already saved as it is entered.
                        Text("Save & Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionPalette(
    questions: List<Question>,
    sections: List<PaletteSection>,
    showHeadings: Boolean,
    currentIndex: Int,
    statusOf: (Question) -> QuestionStatus,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Built once rather than searching the list for every cell drawn.
    val indexOfId = remember(questions) {
        questions.withIndex().associate { (index, question) -> question.id to index }
    }
    Column(modifier.padding(horizontal = 20.dp)) {
        Text("Questions", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LegendDot("Answered", QuestionStatus.ANSWERED)
            LegendDot("Marked", QuestionStatus.MARKED)
            LegendDot("Skipped", QuestionStatus.NOT_ANSWERED)
        }

        HorizontalDivider()

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 56.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            fun cells(group: List<Question>) = group.map { question ->
                question to (indexOfId[question.id] ?: 0)
            }

            if (showHeadings) {
                sections.forEach { section ->
                    item(
                        span = { GridItemSpan(maxLineSpan) },
                        key = "heading-${section.name}",
                    ) {
                        Text(
                            text = section.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                        )
                    }
                    items(cells(section.questions), key = { it.first.id }) { (question, index) ->
                        PaletteCell(
                            number = question.number,
                            status = statusOf(question),
                            isCurrent = index == currentIndex,
                            onClick = { onSelect(index) },
                        )
                    }
                }
            } else {
                items(cells(questions), key = { it.first.id }) { (question, index) ->
                    PaletteCell(
                        number = question.number,
                        status = statusOf(question),
                        isCurrent = index == currentIndex,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun statusColor(status: QuestionStatus): Color {
    val answers = LocalAnswerColors.current
    return when (status) {
        QuestionStatus.ANSWERED -> answers.correct
        QuestionStatus.ANSWERED_AND_MARKED -> answers.marked
        QuestionStatus.MARKED -> answers.marked
        QuestionStatus.NOT_ANSWERED -> answers.unanswered
        QuestionStatus.NOT_VISITED -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
}

@Composable
private fun LegendDot(label: String, status: QuestionStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(statusColor(status)),
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PaletteCell(
    number: Int,
    status: QuestionStatus,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = statusColor(status)
    val onBackground = if (status == QuestionStatus.NOT_VISITED) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color.Black.copy(alpha = 0.82f)
    }

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(
                if (isCurrent) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = onBackground,
            textAlign = TextAlign.Center,
        )
    }
}
