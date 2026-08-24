package com.gatemaster.app

import android.app.Application
import com.gatemaster.app.core.data.ContentRepository
import com.gatemaster.app.core.data.TestRepository

/**
 * Manual dependency container.
 *
 * Deliberately hand-rolled and tiny: it keeps the first working build free of
 * annotation processing. Hilt replaces it once the Room layer lands, and the
 * swap is confined to this file plus the ViewModel factories.
 */
class AppContainer(application: Application) {
    val contentRepository: ContentRepository by lazy {
        ContentRepository(application.assets)
    }

    val testRepository: TestRepository by lazy {
        TestRepository(application.assets, application.filesDir)
    }
}

class GateMasterApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
