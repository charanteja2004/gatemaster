package com.gatemaster.app.ui.papers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.model.Paper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PapersUiState(
    val isLoading: Boolean = true,
    val papers: List<Paper> = emptyList(),
)

class PapersViewModel(
    private val repository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PapersUiState())
    val uiState: StateFlow<PapersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val papers = repository.papers().sortedByDescending { it.year }
            _uiState.update { it.copy(isLoading = false, papers = papers) }
        }
    }
}
