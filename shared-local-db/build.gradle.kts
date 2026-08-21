import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        exclude("build/generated/**")
        exclude("**/build/generated/ksp/**")
        exclude("**/ksp/**")
        exclude { it.file.absolutePath.contains("/build/generated/") }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

android {
    namespace = "com.zhihuminus.shared.localdb"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    defaultConfig {
        minSdk = 27
    }
}

dependencies {
    api("androidx.room:room-runtime:2.8.4")
    api("androidx.room:room-ktx:2.8.4")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    ksp("androidx.room:room-compiler:2.8.4")
}
