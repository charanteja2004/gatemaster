package com.gatemaster.app.ui.papers

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
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatemaster.app.core.model.ContentRef
import com.gatemaster.app.ui.AppViewModelProvider
import com.gatemaster.app.ui.subject.OpenRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PapersScreen(
    onBack: () -> Unit,
    onOpen: (OpenRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PapersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Previous year papers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.papers, key = { it.id }) { paper ->
                    PaperRow(
                        year = paper.year,
                        title = paper.title,
                        sizeBytes = paper.sizeBytes,
                        answerKey = paper.answerKey,
                        onOpenPaper = {
                            onOpen(OpenRequest(paper.title, "Question paper", paper.paper))
                        },
                        onOpenKey = { ref ->
                            onOpen(OpenRequest("GATE ${paper.year} answer key", paper.title, ref))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaperRow(
    year: Int,
    title: String,
    sizeBytes: Long,
    answerKey: ContentRef?,
    onOpenPaper: () -> Unit,
    onOpenKey: (ContentRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onOpenPaper,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("GATE $year", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = buildString {
                        append("Computer Science")
                        if (sizeBytes > 0) {
                            append(" · ")
                            append("%.1f MB".format(sizeBytes / (1024.0 * 1024.0)))
                        }
                        if (answerKey == null) append(" · no answer key")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (answerKey != null) {
                TextButton(onClick = { onOpenKey(answerKey) }) {
                    Icon(
                        Icons.Filled.Key,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text("Key")
                }
            }
        }
    }
}
