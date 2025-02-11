import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "eu.brrm.shared_ui"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        viewBinding = true
        flavorDimensions += listOf("main")
    }
    productFlavors {
        create("app"){
            dimension=flavorDimensions[0]
            minSdk=21
        }
        create("tst") {
            dimension = flavorDimensions.get(0)
            minSdk=23
        }
    }
    buildToolsVersion = "35.0.0"
}

dependencies {
    implementation(libs.kotlin)
    implementation(libs.bundles.base)
    implementation(libs.bundles.ui.components)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
    implementation(project(":mapp-engage-sdk"))

    testImplementation(libs.bundles.test)
    androidTestImplementation(libs.bundles.androidTest)
}