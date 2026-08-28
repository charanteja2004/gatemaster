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

include(":app")

// The sync API. A plain JVM module -- it shares the wire models' shape with
// the app but not its dependencies, and it builds and tests without the
// Android SDK.
include(":server")

// The pre-rewrite Java/XML app is kept on disk at ./legacy for reference and
// asset recovery. It is intentionally NOT part of the build.
