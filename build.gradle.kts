// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        maven("https://jitpack.io")
        mavenCentral()
    }
    dependencies {
        classpath("com.github.tafilovic:central-portal-publisher:2.0.4")
    }
}

plugins {
    id("com.android.application") version "8.10.0" apply false
    id("com.android.library") version "8.10.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

//gradle.projectsEvaluated {
//    tasks.withType<JavaCompile>().configureEach {
//        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
//    }
//}
