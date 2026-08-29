import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // No org.jetbrains.kotlin.android here: AGP 9 has built-in Kotlin support
    // and rejects that plugin. Compiler plugins are still applied normally.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/**
 * Release signing material, kept out of the repository.
 *
 * Create keystore.properties next to settings.gradle.kts with storeFile,
 * storePassword, keyAlias and keyPassword; see the README. When it is absent
 * -- a fresh clone, or CI building a debug APK -- the release build simply
 * goes unsigned rather than failing, so the file is never required just to
 * compile.
 */
val signingProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasSigningMaterial = signingProperties.getProperty("storeFile") != null

android {
    namespace = "com.gatemaster.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // NOTE: this is permanent once the app is first uploaded to Play.
        // Change it now if you own a domain you would rather use.
        applicationId = "com.gatemaster.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 2
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // Where the sync API lives, baked in at build time. A published APK
        // carries the real one (release.yml passes it from a repository
        // variable), so nobody who installs the app is ever asked for a URL.
        //
        // Empty by default, because a fresh clone has nowhere to point at. The
        // account screen then says sync is unavailable rather than failing
        // against a host that does not exist.
        //
        //   ./gradlew :app:assembleRelease -Pgatemaster.syncBaseUrl=https://...
        //
        // Debug builds can also be pointed somewhere per-install from the
        // account screen, which wins over this. That is for running against a
        // server on your own machine; release builds do not offer it.
        buildConfigField(
            "String",
            "SYNC_BASE_URL",
            "\"${providers.gradleProperty("gatemaster.syncBaseUrl").getOrElse("")}\"",
        )
    }

    signingConfigs {
        if (hasSigningMaterial) {
            create("release") {
                storeFile = rootProject.file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    // AGP 9 built-in Kotlin: configured inside android {}, not a top-level
    // kotlin {} block.
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        // For SYNC_BASE_URL below.
        buildConfig = true
    }

    // Schemas are checked in so a migration can be diffed in review rather
    // than discovered by a crash on someone's phone.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    testOptions {
        // ContentIndexTest reads the assets straight off disk rather than out
        // of the APK, so Gradle cannot see them as an input and would report
        // the tests up to date after the content changed. Declaring the folder
        // is what makes "content drift breaks the build" actually true.
        unitTests.all {
            it.inputs.dir(layout.projectDirectory.dir("src/main/assets"))
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
        unitTests.isIncludeAndroidResources = true
        unitTests {
            // android.util.Log is a stub in unit tests and throws unless this
            // is set. The repositories log on the failure paths, which is
            // exactly what the tests exercise.
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    // The bundled study material is already compressed (PDF) or tiny (HTML);
    // leaving PDFs uncompressed lets them be streamed straight from assets.
    androidResources {
        noCompress += "pdf"
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // The wire contract, shared with :server. Neither side can rename a field
    // without breaking the other's compile.
    implementation(project(":protocol"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.layout)
    implementation(libs.compose.material3.adaptive.navigation)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
