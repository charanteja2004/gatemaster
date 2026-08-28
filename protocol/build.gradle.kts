import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Serialization and nothing else. This module is on the Android app's
    // classpath, so anything added here is added to the APK -- and anything
    // JVM-only added here would stop the app compiling at all, which is a
    // useful thing for the build to enforce rather than a comment to remember.
    api(libs.kotlinx.serialization.json)
}
