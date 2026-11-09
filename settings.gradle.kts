@file:Suppress("UnstableApiUsage")
pluginManagement {
    val agpVersion by extra("8.7.3")
    val kotlinVersion by extra("2.0.21")
    val composeBom = "2024.11.00"

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

rootProject.name = "antispy"
include(":app")
