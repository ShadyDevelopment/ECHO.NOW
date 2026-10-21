import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appConfig = rootProject.extra["echoNowConfig"] as java.util.Properties
val generatedDesktopConfig = layout.buildDirectory.dir("generated/desktopConfig/kotlin")
val generateDesktopConfig by tasks.registering {
    val configFile = rootProject.file("config.cfg")
    val outputFile = generatedDesktopConfig.map {
        it.file("com/echonow/platform/DesktopBuildConfiguration.kt")
    }
    inputs.file(configFile)
    outputs.file(outputFile)

    doLast {
        fun String.asKotlinLiteral(): String = buildString {
            append('"')
            for (character in this@asKotlinLiteral) {
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '$' -> append("\\$")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(character)
                }
            }
            append('"')
        }

        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            """
            package com.echonow.platform

            internal object DesktopBuildConfiguration {
                const val appName = ${appConfig.getProperty("APP_NAME", "ECHO NOW").asKotlinLiteral()}
                const val appVersion = ${appConfig.getProperty("APP_VERSION", "0.2.0").asKotlinLiteral()}
                const val appWeb = ${appConfig.getProperty("APP_WEB", "").asKotlinLiteral()}
                const val subdlApiKey = ${appConfig.getProperty("SUBDL_API_KEY", "").asKotlinLiteral()}
                const val tmdbApiKey = ${appConfig.getProperty("THEMOVIEDB_API_KEY", "").asKotlinLiteral()}
                const val tmdbReadAccessToken = ${appConfig.getProperty("THEMOVIEDB_API_READ_ACCESS_TOKEN", "").asKotlinLiteral()}
            }
            """.trimIndent(),
        )
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "com.echonow"
        compileSdk = 37
        minSdk = 23

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.roku.focus.list)
            implementation(libs.datastore.preferences.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.datastore.preferences)
            implementation(libs.ktor.client.cio)
            implementation(libs.gsy.player.compose)
            implementation(libs.gsy.player.exo2)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.exoplayer.dash)
            implementation(libs.android.youtube.player)
        }

        val desktopMain by getting {
            kotlin.srcDir(generatedDesktopConfig)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
                implementation(libs.vlcj)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.ktor.client.mock)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.echonow.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = appConfig.getProperty("APP_NAME", "ECHO NOW").trim()
            packageVersion = appConfig.getProperty("APP_VERSION", "0.2.0").trim()
        }
    }
}

tasks.matching { it.name == "compileKotlinDesktop" }.configureEach {
    dependsOn(generateDesktopConfig)
}
