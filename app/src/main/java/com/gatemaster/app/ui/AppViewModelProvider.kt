package com.gatemaster.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gatemaster.app.GateMasterApplication
import com.gatemaster.app.ui.home.HomeViewModel
import com.gatemaster.app.ui.papers.PapersViewModel
import com.gatemaster.app.ui.search.SearchViewModel
import com.gatemaster.app.ui.subject.SubjectViewModel

/**
 * ViewModel wiring for the manual container. Replaced wholesale by
 * `@HiltViewModel` once Hilt is added; nothing else has to change.
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer { HomeViewModel(app().container.contentRepository) }
        initializer { PapersViewModel(app().container.contentRepository) }
        initializer { SearchViewModel(app().container.contentRepository) }
        initializer {
            SubjectViewModel(
                repository = app().container.contentRepository,
                savedStateHandle = createSavedStateHandle(),
            )
        }
    }
}

private fun CreationExtras.app(): GateMasterApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GateMasterApplication
