import com.google.common.collect.ImmutableList

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

val sdkVersion = project.properties["VERSION"]

android {
    namespace = "com.appoxee"
    compileSdk = 34

    lint {
        targetSdk = 34
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

    packaging {
        resources.excludes.addAll(
            ImmutableList.of(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
        )
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
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.ui.components)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.exoplayer)
    implementation(libs.bundles.datastore)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    testImplementation(libs.bundles.test)

    androidTestImplementation(libs.bundles.androidTest)
}