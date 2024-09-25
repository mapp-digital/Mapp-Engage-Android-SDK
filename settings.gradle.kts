@file:Suppress("UnstableApiUsage")

import org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(FAIL_ON_PROJECT_REPOS)
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

            // chrome tabs - browser
            library("browser","androidx.browser:browser:1.5.0")

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

            //test
            library("junit4","junit:junit:4.13.2")
            library("jupiter","org.junit.jupiter:junit-jupiter:5.10.1")
            library("mockk","io.mockk:mockk:1.13.8")
            library("truth","com.google.truth:truth:1.2.0")
            library("json","org.json:json:20180813")
            library("okhttp3-mockwebserver","com.squareup.okhttp3:mockwebserver:4.11.0")
            library( "kotlinx-coroutines-test","org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")

            //androidTest
            library("androidx-test-junit","androidx.test.ext:junit:1.1.5")
            library("androidx-test-espresso","androidx.test.espresso:espresso-core:3.5.1")
            library("androidx-test-core","androidx.test:core:1.5.0")
            library("androidx-test-mockk","io.mockk:mockk-android:1.13.8")

            //bundles
            bundle("base", listOf("core-ktx", "appcompat","material","lifecycle-runtime-ktx","coroutines-ktx"))
            bundle("datastore", listOf("datastore-preferences"))
            bundle("ui-components", listOf("recycler","constraintlayout"))
            bundle("coil", listOf("coil","coil-gif"))
            bundle("exoplayer", listOf("media3-exoplayer","media3-exoplayer-dash","media3-ui"))
            bundle("test", listOf("junit4","jupiter","mockk","truth","json","okhttp3-mockwebserver", "kotlinx-coroutines-test"))
            bundle("androidTest", listOf("androidx-test-junit","androidx-test-espresso","androidx-test-core", "androidx-test-mockk", "truth","jupiter"))
        }
    }
}

rootProject.name = "Mapp-Engage-v7"
include(":sample-kotlin")
include(":mapp-engage-sdk")
include(":sample-java")
include(":shared-ui")
include("dependencies.gradle.kts")
