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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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
    }
    //android.buildFeatures.buildConfig = true
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.ui.components)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.exoplayer)
    implementation(libs.bundles.datastore)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20180813")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation("com.google.truth:truth:1.1.5")
    androidTestImplementation("androidx.test:core:1.5.0")
}