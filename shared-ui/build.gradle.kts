import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
}

extensions.configure<LibraryExtension> {
    namespace = "eu.brrm.shared_ui"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = 23
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

    buildFeatures {
        viewBinding = true
        flavorDimensions += listOf("main")
    }
    productFlavors {
        create("prod") {
            dimension = flavorDimensions[0]
            minSdk = 23
        }
        create("tst") {
            dimension = flavorDimensions.get(0)
            minSdk = 23
        }
    }

    packaging {
        resources {
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.kotlin)
    implementation(libs.bundles.base)
    implementation(libs.bundles.ui.components)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    testImplementation(libs.bundles.test)
    androidTestImplementation(libs.bundles.android.test)
}

apply(from = "$rootDir/engage-dependency.gradle")