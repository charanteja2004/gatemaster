import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
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

application {
    mainClass.set("com.gatemaster.server.MainKt")
}

dependencies {
    // The wire contract, shared with the Android app. A renamed field is a
    // compile error on both sides rather than a 400 a user discovers.
    api(project(":protocol"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.flyway.core)
    // Flyway 10 split each database into its own module; without this one it
    // refuses to run against Postgres at all.
    runtimeOnly(libs.flyway.postgresql)
    implementation(libs.hikaricp)
    runtimeOnly(libs.postgresql)

    implementation(libs.bcrypt)
    implementation(libs.logback.classic)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotlinx.coroutines.test)
    // The suite runs against H2 in PostgreSQL mode, so `./gradlew :server:test`
    // needs neither Docker nor a database.
    testImplementation(libs.h2)
}

tasks.withType<Test>().configureEach {
    useJUnit()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
