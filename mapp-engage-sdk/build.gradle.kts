import com.google.common.collect.ImmutableList

plugins {
    id("com.android.library")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.android")
}

val sdkVersion = project.properties["VERSION"]

android {
    namespace = "com.appoxee"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    lint {
        targetSdk = 35
    }

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField(type = "String", name = "VERSION_NAME", "\"${sdkVersion}\"")
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
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }

    packaging {
        resources {
            pickFirsts += "META-INF/LICENSE.md"
            pickFirsts +="META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation(libs.kotlin)
    implementation(libs.bundles.base)
    implementation(libs.bundles.ui.components)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.media3)
    implementation(libs.datastore.preferences)
    implementation(libs.browser)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.play.services.location)
    implementation(libs.work.manager)
    implementation(libs.androidx.lifecycle.process)

    testImplementation(libs.bundles.test)
    androidTestImplementation(libs.bundles.android.test)
}