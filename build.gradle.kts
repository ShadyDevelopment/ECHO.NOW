import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}

val appConfigFile = rootProject.file("config.cfg")
require(appConfigFile.isFile) { "Missing config.cfg at the project root." }
val appConfig = Properties().apply {
    appConfigFile.inputStream().use(::load)
}
appConfig.stringPropertyNames().forEach { key ->
    appConfig.setProperty(key, appConfig.getProperty(key).trim().removeSurrounding("\"").removeSurrounding("'"))
}
rootProject.extra["echoNowConfig"] = appConfig
