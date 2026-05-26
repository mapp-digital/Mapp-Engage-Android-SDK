import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

extensions.configure<ApplicationExtension> {
    namespace = "com.mapp.engagesample"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.appoxee.example"
        minSdk = 23
        targetSdk = 36
        versionCode = 30
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "MAPP_SDK_KEY", "\"${localProperties["mapp.sdk.key"]}\"")
        buildConfigField("String", "MAPP_APP_ID", "\"${localProperties["mapp.app.id"]}\"")
        buildConfigField("String", "MAPP_TENANT_ID", "\"${localProperties["mapp.tenant.id"]}\"")
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    flavorDimensions += listOf("main")
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
    implementation(project(mapOf("path" to ":shared-ui")))
    implementation(libs.kotlin)
    implementation(libs.bundles.base)
    implementation(libs.bundles.ui.components)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.bundles.coil)
    implementation(libs.viewmodel.ktx)

    testImplementation(libs.bundles.test)
    androidTestImplementation(libs.bundles.android.test)
}

apply(from = "$rootDir/engage-dependency.gradle")