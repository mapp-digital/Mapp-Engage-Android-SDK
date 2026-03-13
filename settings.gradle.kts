@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Mapp-Engage-v7"

if (providers.gradleProperty("useLocalEngage").orNull == "true") {
    include(":mapp-engage-sdk")
}

include(":sample-kotlin")
include(":sample-java")
include(":shared-ui")
