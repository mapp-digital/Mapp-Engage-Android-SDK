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

    versionCatalogs {
        create("libs"){
            // base
            library("core-ktx","androidx.core:core-ktx:1.12.0")
            library("appcompat","androidx.appcompat:appcompat:1.6.1")
            library("material","com.google.android.material:material:1.11.0")
            library("lifecycle-runtime-ktx","androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
            library("coroutines-ktx","org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

            // ui components
            library("recycler","androidx.recyclerview:recyclerview:1.3.2")
            library("constraintlayout","androidx.constraintlayout:constraintlayout:2.1.4")

            // firebase
            library("firebase-bom","com.google.firebase:firebase-bom:32.4.0")
            library("firebase-messaging-ktx","com.google.firebase","firebase-messaging-ktx").withoutVersion()

            // datastore prefs
            library("datastore-preferences","androidx.datastore:datastore-preferences:1.0.0")

            //coild
            library("coil","io.coil-kt:coil:2.5.0")
            library("coil-gif","io.coil-kt:coil-gif:2.5.0")

            //exoplayer media 3
            library("media3-exoplayer","androidx.media3:media3-exoplayer:1.2.0")
            library("media3-exoplayer-dash","androidx.media3:media3-exoplayer-dash:1.2.0")
            library("media3-ui","androidx.media3:media3-ui:1.2.0")

            //bundles
            bundle("base", listOf("core-ktx", "appcompat","material","lifecycle-runtime-ktx","coroutines-ktx"))
            bundle("datastore", listOf("datastore-preferences"))
            bundle("ui-components", listOf("recycler","constraintlayout"))
            bundle("coil", listOf("coil","coil-gif"))
            bundle("exoplayer", listOf("media3-exoplayer","media3-exoplayer-dash","media3-ui"))

        }
    }
}

rootProject.name = "EngageSample"
include(":sample-kotlin")
include(":mapp-engage-sdk")
include(":sample-java")
include(":shared-ui")
include("dependencies.gradle.kts")
