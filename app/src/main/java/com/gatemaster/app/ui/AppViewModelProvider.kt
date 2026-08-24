package com.gatemaster.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gatemaster.app.GateMasterApplication
import com.gatemaster.app.ui.branch.BranchPickerViewModel
import com.gatemaster.app.ui.home.HomeViewModel
import com.gatemaster.app.ui.papers.PapersViewModel
import com.gatemaster.app.ui.reader.ReaderViewModel
import com.gatemaster.app.ui.search.SearchViewModel
import com.gatemaster.app.ui.subject.SubjectViewModel
import com.gatemaster.app.ui.test.TestListViewModel
import com.gatemaster.app.ui.test.TestPlayerViewModel

/**
 * ViewModel wiring for the manual container. Replaced wholesale by
 * `@HiltViewModel` once Hilt is added; nothing else has to change.
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(
                repository = app().container.contentRepository,
                preferences = app().container.userPreferences,
            )
        }
        initializer {
            BranchPickerViewModel(
                repository = app().container.contentRepository,
                preferences = app().container.userPreferences,
            )
        }
        initializer {
            PapersViewModel(
                repository = app().container.contentRepository,
                preferences = app().container.userPreferences,
            )
        }
        initializer {
            SearchViewModel(
                repository = app().container.contentRepository,
                preferences = app().container.userPreferences,
            )
        }
        initializer { TestListViewModel(app().container.testRepository) }
        initializer {
            TestPlayerViewModel(
                repository = app().container.testRepository,
                savedStateHandle = createSavedStateHandle(),
            )
        }
        initializer {
            ReaderViewModel(
                repository = app().container.contentRepository,
                preferences = app().container.userPreferences,
                savedStateHandle = createSavedStateHandle(),
            )
        }
        initializer {
            SubjectViewModel(
                repository = app().container.contentRepository,
                preferences = app().container.userPreferences,
                savedStateHandle = createSavedStateHandle(),
            )
        }
    }
}

private fun CreationExtras.app(): GateMasterApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GateMasterApplication
