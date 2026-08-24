// AGP 9 ships with its own Kotlin Gradle Plugin (2.2.x). We compile against a
// newer Kotlin, so pin KGP on the buildscript classpath — this is the upgrade
// path documented at kotl.in/gradle/agp-built-in-kotlin. Without it, the
// Compose and serialization compiler plugins would run against a different
// Kotlin than the one compiling the sources.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

// Plugins are declared here (without applying them) so every module resolves
// the same version from gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
