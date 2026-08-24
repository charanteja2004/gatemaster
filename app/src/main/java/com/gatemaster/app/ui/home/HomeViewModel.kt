package com.gatemaster.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val subjects: List<Subject> = emptyList(),
    val paperCount: Int = 0,
    val latestPaperYear: Int? = null,
    val errorMessage: String? = null,
) {
    val totalTopics: Int get() = subjects.sumOf { it.itemCount }
}

class HomeViewModel(
    private val repository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.index()
                .onSuccess { index ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            subjects = index.subjects.sortedBy { s -> s.order },
                            paperCount = index.papers.size,
                            latestPaperYear = index.papers.maxOfOrNull { p -> p.year },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Study material could not be loaded. " +
                                (error.message ?: "Please reinstall the app."),
                        )
                    }
                }
        }
    }
}
