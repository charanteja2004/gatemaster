package com.gatemaster.app.ui.subject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.model.Subject
import com.gatemaster.app.navigation.SubjectRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which group of material the user is looking at within a subject. */
enum class SubjectTab(val label: String) {
    TOPICS("Topics"),
    HANDOUTS("Handouts"),
    REVISION("Revision"),
    SYLLABUS("Syllabus"),
}

data class SubjectUiState(
    val isLoading: Boolean = true,
    val subject: Subject? = null,
    val selectedTab: SubjectTab = SubjectTab.TOPICS,
    val notFound: Boolean = false,
) {
    /** Only offer a tab when there is something behind it. */
    val availableTabs: List<SubjectTab>
        get() {
            val s = subject ?: return emptyList()
            return buildList {
                if (s.topics.isNotEmpty()) add(SubjectTab.TOPICS)
                if (s.referenceNotes.isNotEmpty()) add(SubjectTab.HANDOUTS)
                if (s.shortNotes != null) add(SubjectTab.REVISION)
                if (s.syllabus.isNotEmpty()) add(SubjectTab.SYLLABUS)
            }
        }
}

class SubjectViewModel(
    private val repository: ContentRepository,
    private val preferences: UserPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val subjectId: String = savedStateHandle.toRoute<SubjectRoute>().subjectId

    private val _uiState = MutableStateFlow(SubjectUiState())
    val uiState: StateFlow<SubjectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val branchId = preferences.branchId.first()
            val subject = repository.subject(branchId, subjectId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    subject = subject,
                    notFound = subject == null,
                    selectedTab = firstTabFor(subject),
                )
            }
        }
    }

    private fun firstTabFor(subject: Subject?): SubjectTab = when {
        subject == null -> SubjectTab.TOPICS
        subject.topics.isNotEmpty() -> SubjectTab.TOPICS
        subject.referenceNotes.isNotEmpty() -> SubjectTab.HANDOUTS
        subject.shortNotes != null -> SubjectTab.REVISION
        else -> SubjectTab.SYLLABUS
    }

    fun selectTab(tab: SubjectTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
