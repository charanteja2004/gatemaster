package com.gatemaster.app.ui.subject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.StudyProgressRepository
import com.gatemaster.app.core.data.TestRepository
import com.gatemaster.app.core.data.TopicProgress
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
    PRACTICE("Practice"),
    HANDOUTS("Handouts"),
    REVISION("Revision"),
    SYLLABUS("Syllabus"),
}

/** One topic on the practice tab, with the size of the set behind it. */
data class TopicPractice(
    val topicId: String,
    val title: String,
    val questionCount: Int,
) {
    /** Below the threshold a set would be the same three questions every time. */
    val isReady: Boolean get() = questionCount >= MIN_QUESTIONS

    companion object {
        const val MIN_QUESTIONS = 3
    }
}

data class SubjectUiState(
    val isLoading: Boolean = true,
    val subject: Subject? = null,
    val selectedTab: SubjectTab = SubjectTab.TOPICS,
    val notFound: Boolean = false,
    val progress: Map<String, TopicProgress> = emptyMap(),
    /** Topics with enough questions to offer a practice test. */
    val practisableTopics: Set<String> = emptySet(),
    val subjectQuestionCount: Int = 0,
    /** Every topic that has any questions at all, ready or not. */
    val topicPractice: List<TopicPractice> = emptyList(),
) {
    val readyTopicPractice: List<TopicPractice> get() = topicPractice.filter { it.isReady }

    /** Topics with a question or two — worth saying so rather than hiding them. */
    val pendingTopicCount: Int get() = topicPractice.count { !it.isReady }

    fun isRead(topicId: String): Boolean = progress[topicId]?.isRead == true

    fun isBookmarked(topicId: String): Boolean = progress[topicId]?.bookmarked == true

    val readCount: Int get() = subject?.topics?.count { isRead(it.id) } ?: 0

    /** Only offer a tab when there is something behind it. */
    val availableTabs: List<SubjectTab>
        get() {
            val s = subject ?: return emptyList()
            return buildList {
                if (s.topics.isNotEmpty()) add(SubjectTab.TOPICS)
                if (subjectQuestionCount > 0) add(SubjectTab.PRACTICE)
                if (s.referenceNotes.isNotEmpty()) add(SubjectTab.HANDOUTS)
                if (s.shortNotes != null) add(SubjectTab.REVISION)
                if (s.syllabus.isNotEmpty()) add(SubjectTab.SYLLABUS)
            }
        }
}

class SubjectViewModel(
    private val repository: ContentRepository,
    private val preferences: UserPreferences,
    private val studyProgress: StudyProgressRepository,
    private val testRepository: TestRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val subjectId: String = savedStateHandle.toRoute<SubjectRoute>().subjectId

    private val _uiState = MutableStateFlow(SubjectUiState())
    val uiState: StateFlow<SubjectUiState> = _uiState.asStateFlow()

    init {
        // Subject and question counts load together rather than in parallel:
        // the practice list needs the topic titles from the subject to say
        // anything more useful than an id.
        viewModelScope.launch {
            val branchId = preferences.branchId.first()
            val subject = repository.subject(branchId, subjectId)
            val counts = testRepository.topicQuestionCounts(subjectId)
            val practice = subject?.topics.orEmpty().mapNotNull { topic ->
                counts[topic.id]?.let { count -> TopicPractice(topic.id, topic.title, count) }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    subject = subject,
                    notFound = subject == null,
                    topicPractice = practice,
                    practisableTopics = practice.filter(TopicPractice::isReady)
                        .map(TopicPractice::topicId)
                        .toSet(),
                    subjectQuestionCount = testRepository.questionCount(subjectId),
                    selectedTab = firstTabFor(subject),
                )
            }
        }
        viewModelScope.launch {
            studyProgress.load()
            studyProgress.progress.collect { all ->
                _uiState.update { it.copy(progress = all) }
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
