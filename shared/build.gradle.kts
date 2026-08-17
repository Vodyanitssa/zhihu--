import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
}

configurations.configureEach {
    resolutionStrategy {
        force("org.jetbrains.compose.material3:material3:1.10.0-alpha05")
    }
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

tasks.withType<KtLintFormatTask>().configureEach {
    exclude("**/generated/**")
    exclude("build/generated/**")
    exclude("**/build/generated/ksp/**")
    exclude("**/ksp/**")
    exclude { it.file.absolutePath.contains("/build/generated/") }
}

tasks
    .withType<GenerateReportsTask>()
    .matching { it.name in setOf("ktlintMainSourceSetFormat", "ktlintTestSourceSetFormat") }
    .configureEach {
        enabled = false
    }

mapOf(
    "runKtlintFormatOverMainSourceSet" to "src/main/kotlin",
    "runKtlintFormatOverTestSourceSet" to "src/test/kotlin",
).forEach { (taskName, sourcePath) ->
    tasks.withType<KtLintFormatTask>().matching { it.name == taskName }.configureEach {
        setSource(
            fileTree(sourcePath) {
                include("**/*.kt")
            },
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

android {
    namespace = "com.github.zly2006.zhihu.shared"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.11.1"
    }
}

val composeVersion = "1.11.1"
val ktorVersion = "3.5.0"
val coilVersion = "3.5.0"

dependencies {
    api(project(":shared-local-db"))
    implementation(project(":markdown-parser"))
    implementation(project(":markdown-renderer"))

    // Compose
    implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
    implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
    implementation("org.jetbrains.compose.material3:material3:1.10.0-alpha05")
    implementation("org.jetbrains.compose.ui:ui:$composeVersion")
    implementation("org.jetbrains.compose.ui:ui-graphics:$composeVersion")
    implementation("org.jetbrains.compose.animation:animation:$composeVersion")
    implementation("org.jetbrains.compose.animation:animation-core:$composeVersion")
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Ktor
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")

    // Other
    implementation("com.materialkolor:material-kolor:4.1.1")
    implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
    implementation("io.coil-kt.coil3:coil-compose:$coilVersion")
    implementation("io.coil-kt.coil3:coil-network-core:$coilVersion")
    implementation("io.coil-kt.coil3:coil-network-ktor3-android:$coilVersion")
    implementation("io.coil-kt.coil3:coil-gif:$coilVersion")
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("com.mikepenz:aboutlibraries-compose-m3:15.0.0")
    implementation("io.github.zly2006:latex-renderer-android:0.0.1-alpha5")

    // Android-specific
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.browser:browser:1.10.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.11.0")
    implementation("androidx.media:media:1.7.1")
    implementation("androidx.webkit:webkit:1.16.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.4")
    implementation("me.saket.telephoto:zoomable-image-coil3:0.19.0")
    implementation("org.jsoup:jsoup:1.22.2")

    // Test
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}
