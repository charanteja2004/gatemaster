package com.gatemaster.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gatemaster.app.BuildConfig
import com.gatemaster.app.GateMasterApplication
import com.gatemaster.app.ui.account.AccountViewModel
import com.gatemaster.app.ui.branch.BranchPickerViewModel
import com.gatemaster.app.ui.home.HomeViewModel
import com.gatemaster.app.ui.papers.PapersViewModel
import com.gatemaster.app.ui.progress.ProgressViewModel
import com.gatemaster.app.ui.reader.ReaderViewModel
import com.gatemaster.app.ui.search.SearchViewModel
import com.gatemaster.app.ui.settings.SettingsViewModel
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
                studyProgress = app().container.studyProgress,
            )
        }
        initializer {
            AccountViewModel(
                auth = app().container.authRepository,
                preferences = app().container.userPreferences,
                sync = app().container.syncManager,
                // A released app knows its own server; only a developer needs
                // to point one somewhere else.
                canChooseServer = BuildConfig.DEBUG,
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
        initializer {
            SettingsViewModel(
                repository = app().container.contentRepository,
                preferences = app().container.userPreferences,
                auth = app().container.authRepository,
                canChooseServer = BuildConfig.DEBUG,
            )
        }
        initializer {
            ProgressViewModel(
                progress = app().container.progressRepository,
                content = app().container.contentRepository,
                preferences = app().container.userPreferences,
            )
        }
        initializer {
            TestListViewModel(
                repository = app().container.testRepository,
                contentRepository = app().container.contentRepository,
                preferences = app().container.userPreferences,
                progress = app().container.progressRepository,
            )
        }
        initializer {
            TestPlayerViewModel(
                repository = app().container.testRepository,
                progress = app().container.progressRepository,
                savedStateHandle = createSavedStateHandle(),
                requestSync = app().container::requestSync,
            )
        }
        initializer {
            ReaderViewModel(
                repository = app().container.contentRepository,
                preferences = app().container.userPreferences,
                studyProgress = app().container.studyProgress,
                savedStateHandle = createSavedStateHandle(),
            )
        }
        initializer {
            SubjectViewModel(
                repository = app().container.contentRepository,
                preferences = app().container.userPreferences,
                studyProgress = app().container.studyProgress,
                testRepository = app().container.testRepository,
                savedStateHandle = createSavedStateHandle(),
            )
        }
    }
}

private fun CreationExtras.app(): GateMasterApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GateMasterApplication
