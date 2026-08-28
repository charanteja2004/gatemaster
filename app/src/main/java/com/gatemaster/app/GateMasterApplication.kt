package com.gatemaster.app

import android.app.Application
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.ProgressRepository
import com.gatemaster.app.core.data.asAssetSource
import com.gatemaster.app.core.data.StudyProgressRepository
import com.gatemaster.app.core.data.TestRepository
import com.gatemaster.app.core.data.UserPreferences
import com.gatemaster.app.core.data.auth.AuthRepository
import com.gatemaster.app.core.data.auth.SyncApi
import com.gatemaster.app.core.data.auth.TokenStore
import com.gatemaster.app.core.data.db.GateMasterDatabase
import com.gatemaster.app.core.data.sync.SyncManager
import com.gatemaster.app.core.data.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
class AppContainer(private val application: Application) {
    val contentRepository: ContentRepository by lazy {
        ContentRepository(application.assets.asAssetSource())
    }

    val testRepository: TestRepository by lazy {
        TestRepository(
            assets = application.assets.asAssetSource(),
            filesDir = application.filesDir,
            // Adaptive practice reads the same attempt history the Progress
            // tab already aggregates; nothing extra is recorded for it.
            topicHistory = { GateMasterDatabase.get(application).attemptDao().topicHistory() },
            subjectWeights = { contentRepository.subjectWeights(userPreferences.branchId.first()) },
        )
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

    val tokenStore: TokenStore by lazy { TokenStore(application) }

    /**
     * The sync server for this install, or null when there is none.
     *
     * A per-install override beats the value the build was compiled with, so
     * one APK can be pointed at a local server, a deployed one, or nothing at
     * all without rebuilding.
     */
    private suspend fun syncBaseUrl(): String? {
        val override = userPreferences.syncBaseUrlOverride.first()
        return override.ifBlank { BuildConfig.SYNC_BASE_URL }.ifBlank { null }
    }

    val syncApi: SyncApi by lazy {
        SyncApi(baseUrl = ::syncBaseUrl, tokens = tokenStore)
    }

    val syncManager: SyncManager by lazy {
        SyncManager(
            api = syncApi,
            studyProgress = studyProgress,
            dao = GateMasterDatabase.get(application).attemptDao(),
            tokens = tokenStore,
        )
    }

    /**
     * Asks for a sync as soon as there is a network.
     *
     * A lambda rather than the worker itself, so the ViewModels that call it
     * stay free of Context and of WorkManager -- and so a test can pass a
     * counter instead of scheduling real background work.
     */
    fun requestSync() = SyncWorker.syncNow(application)

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            api = syncApi,
            tokens = tokenStore,
            serverConfigured = userPreferences.syncBaseUrlOverride.map { override ->
                override.ifBlank { BuildConfig.SYNC_BASE_URL }.isNotBlank()
            },
        )
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
