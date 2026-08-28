pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GateMaster"

// The wire contract: request and response types, and nothing else. A plain
// Kotlin module, so both the Android app and the JVM server can depend on it
// and the two can never drift apart on what a field is called.
include(":protocol")

include(":app")

// The sync API. A plain JVM module -- it shares the wire models' shape with
// the app but not its dependencies, and it builds and tests without the
// Android SDK.
include(":server")

// The pre-rewrite Java/XML app is kept on disk at ./legacy for reference and
// asset recovery. It is intentionally NOT part of the build.
