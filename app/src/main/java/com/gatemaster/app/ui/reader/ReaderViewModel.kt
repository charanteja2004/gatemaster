package com.gatemaster.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.model.Topic
import com.gatemaster.app.navigation.ReaderRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val previous: Topic? = null,
    val next: Topic? = null,
    val subjectName: String = "",
    /** WebView text zoom as a percentage; 100 is the system default. */
    val textZoom: Int = DEFAULT_ZOOM,
    /** 0f..1f, how far down the article the reader has scrolled. */
    val progress: Float = 0f,
) {
    val canShrink: Boolean get() = textZoom > MIN_ZOOM
    val canGrow: Boolean get() = textZoom < MAX_ZOOM

    companion object {
        const val DEFAULT_ZOOM = 100
        const val MIN_ZOOM = 80
        const val MAX_ZOOM = 180
        const val ZOOM_STEP = 10
    }
}

/**
 * Backs the article reader.
 *
 * Its job beyond rendering is to make a long article feel finite: how far in
 * you are, and what comes next in the subject, so studying is a sequence rather
 * than a series of returns to a list.
 */
class ReaderViewModel(
    private val repository: ContentRepository,
    private val preferences: UserPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route: ReaderRoute = savedStateHandle.toRoute()

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(textZoom = preferences.readerTextZoom.first()) }
            loadNeighbours()
        }
    }

    private suspend fun loadNeighbours() {
        val subjectId = route.subjectId ?: return
        val topicId = route.topicId ?: return
        val branchId = preferences.branchId.first()
        val subject = repository.subject(branchId, subjectId) ?: return

        val index = subject.topics.indexOfFirst { it.id == topicId }
        if (index < 0) return

        _uiState.update {
            it.copy(
                subjectName = subject.name,
                previous = subject.topics.getOrNull(index - 1),
                next = subject.topics.getOrNull(index + 1),
            )
        }
    }

    fun onProgress(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped != _uiState.value.progress) {
            _uiState.update { it.copy(progress = clamped) }
        }
    }

    fun grow() = setZoom(_uiState.value.textZoom + ReaderUiState.ZOOM_STEP)

    fun shrink() = setZoom(_uiState.value.textZoom - ReaderUiState.ZOOM_STEP)

    private fun setZoom(value: Int) {
        val clamped = value.coerceIn(ReaderUiState.MIN_ZOOM, ReaderUiState.MAX_ZOOM)
        _uiState.update { it.copy(textZoom = clamped) }
        viewModelScope.launch { preferences.setReaderTextZoom(clamped) }
    }
}
