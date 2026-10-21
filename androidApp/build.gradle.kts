plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val appConfig = rootProject.extra["echoNowConfig"] as java.util.Properties
fun String.asBuildConfigLiteral(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val configuredAppName = appConfig.getProperty("APP_NAME", "ECHO NOW").trim()
val configuredVersion = appConfig.getProperty("APP_VERSION", "0.2.0").trim()
val versionCodeValue = configuredVersion
    .substringBefore('-')
    .split('.')
    .take(3)
    .map { it.toIntOrNull() ?: 0 }
    .let { parts ->
        val (major, minor, patch) = (parts + List(3 - parts.size) { 0 }).take(3)
        major * 10000 + minor * 100 + patch
    }

android {
    namespace = "com.echonow.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.echonow"
        minSdk = 23
        targetSdk = 36
        versionCode = versionCodeValue.coerceAtLeast(1)
        versionName = configuredVersion
        manifestPlaceholders["appName"] = configuredAppName
        buildConfigField("String", "APP_NAME", configuredAppName.asBuildConfigLiteral())
        buildConfigField("String", "APP_VERSION", configuredVersion.asBuildConfigLiteral())
        buildConfigField("String", "APP_WEB", appConfig.getProperty("APP_WEB", "").trim().asBuildConfigLiteral())
        buildConfigField("String", "SUBDL_API_KEY", appConfig.getProperty("SUBDL_API_KEY", "").trim().asBuildConfigLiteral())
        buildConfigField("String", "THEMOVIEDB_API_KEY", appConfig.getProperty("THEMOVIEDB_API_KEY", "").trim().asBuildConfigLiteral())
        buildConfigField(
            "String",
            "THEMOVIEDB_API_READ_ACCESS_TOKEN",
            appConfig.getProperty("THEMOVIEDB_API_READ_ACCESS_TOKEN", "").trim().asBuildConfigLiteral(),
        )
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
}
