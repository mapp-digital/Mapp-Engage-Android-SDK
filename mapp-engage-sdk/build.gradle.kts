import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("maven-publish")
    id("central.portal.publisher")
}

val VERSION = project.findProperty("VERSION_NAME") as String
val PUBLISHED_GROUP_ID = project.findProperty("GROUP") as String
val ARTIFACT = project.findProperty("POM_ARTIFACT_ID") as String?
val LIBRARY_NAME = project.findProperty("POM_NAME") as String?
val LIBRARY_DESC = project.findProperty("POM_DESCRIPTION") as String?
val DEVELOPER_NAME = project.findProperty("POM_DEVELOPER_NAME") as String?
val DEVELOPER_URL = project.findProperty("POM_DEVELOPER_URL") as String?
val DEVELOPER_ID = project.findProperty("POM_DEVELOPER_ID") as String?
val LICENSE_NAME = project.findProperty("POM_LICENSE_NAME") as String?
val LICENSE_URL = project.findProperty("POM_LICENSE_URL") as String?
val GIT_URL = project.findProperty("POM_URL") as String?
val GIT_DEVELOPER_CONNECTION = project.findProperty("POM_SCM_DEV_CONNECTION") as String?
val GIT_CONNECTION = "scm:git:$GIT_URL"

android {
    namespace = "com.appoxee.sdk"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    lint {
        targetSdk = 36
        checkReleaseBuilds = false
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        minSdk = 23
        buildConfigField(type = "String", name = "VERSION_NAME", "\"${VERSION}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        flavorDimensions += listOf("main")
    }

    productFlavors {
        create("prod") {
            dimension = flavorDimensions[0]
        }
        create("tst") {
            dimension = flavorDimensions[0]
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }

    packaging {
        resources {
            pickFirsts += "META-INF/LICENSE.md"
            pickFirsts += "META-INF/LICENSE-notice.md"
        }
    }

    publishing {
        singleVariant("prodRelease") {}
    }
}

tasks.withType<Test>().configureEach {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
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

centralPortalPublisher {
    componentName = "prodRelease"
    groupId = PUBLISHED_GROUP_ID
    artifactId = ARTIFACT
    version = VERSION
    flavorName = "prod"
}

//tasks.configureEach {
//    if (name.contains("debug", ignoreCase = true)) {
//        enabled = false
//    }
//}

//tasks.configureEach {
//    if (name.contains("test", ignoreCase = true)) {
//        enabled = false
//    }
//}