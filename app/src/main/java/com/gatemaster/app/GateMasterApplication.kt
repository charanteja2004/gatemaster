package com.gatemaster.app

import android.app.Application
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.ProgressRepository
import com.gatemaster.app.core.data.asAssetSource
import com.gatemaster.app.core.data.StudyProgressRepository
import com.gatemaster.app.core.data.TestRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.data.db.GateMasterDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container.
 *
 * Deliberately hand-rolled and tiny: it keeps the first working build free of
 * annotation processing. Hilt replaces it once the Room layer lands, and the
 * swap is confined to this file plus the ViewModel factories.
 */
class AppContainer(application: Application) {
    val contentRepository: ContentRepository by lazy {
        ContentRepository(application.assets.asAssetSource())
    }

    val testRepository: TestRepository by lazy {
        TestRepository(application.assets.asAssetSource(), application.filesDir)
    }

    val userPreferences: UserPreferences by lazy {
        UserPreferences(application)
    }

    val studyProgress: StudyProgressRepository by lazy {
        StudyProgressRepository(application.filesDir)
    }

    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(GateMasterDatabase.get(application).attemptDao())
    }
}

class GateMasterApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Attempts recorded before there was a database would otherwise
        // vanish from the history the first time this version runs.
        scope.launch {
            container.progressRepository.importLegacyHistory(
                container.testRepository.history(),
            )
        }
    }
}
