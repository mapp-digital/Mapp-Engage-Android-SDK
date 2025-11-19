// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        maven("https://jitpack.io")
        mavenCentral()
    }
    dependencies {
        classpath("com.github.tafilovic:central-portal-publisher:2.0.7")
    }
}

plugins {
    id("com.android.application") version "8.13.1" apply false
    id("com.android.library") version "8.13.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}
