import java.util.Properties

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

val appConfigFile = file("config.cfg")
require(appConfigFile.isFile) { "Missing config.cfg at the project root." }
val appConfig = Properties().apply {
    appConfigFile.inputStream().use(::load)
}
appConfig.stringPropertyNames().forEach { key ->
    appConfig.setProperty(key, appConfig.getProperty(key).trim().removeSurrounding("\"").removeSurrounding("'"))
}

rootProject.name = "ECHO-NOW"
include(":composeApp")
include(":androidApp")
