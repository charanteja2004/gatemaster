package com.gatemaster.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.SearchHit
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchHit> = emptyList(),
    val isSearching: Boolean = false,
) {
    val showEmptyState: Boolean
        get() = query.trim().length >= 2 && !isSearching && results.isEmpty()
}

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query ->
                    val hits = repository.search(query)
                    _uiState.update { it.copy(results = hits, isSearching = false) }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, isSearching = query.trim().length >= 2) }
        queryFlow.value = query
    }

    fun clear() {
        _uiState.update { SearchUiState() }
        queryFlow.value = ""
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 180L
    }
}
