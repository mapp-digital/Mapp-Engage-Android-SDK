plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
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
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    lint {
        targetSdk = 35
    }

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

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
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        //flavorDimensions += listOf("main")
    }

//    productFlavors {
//        create("app"){
//            dimension=flavorDimensions[0]
//            minSdk=21
//        }
//        create("tst") {
//            dimension = flavorDimensions.get(0)
//            minSdk=23
//        }
//    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }

    packaging {
        resources {
            pickFirsts += "META-INF/LICENSE.md"
            pickFirsts += "META-INF/LICENSE-notice.md"
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

// Task for publishing to Central Portal
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("engageSdk") {
                from(components["release"])

                groupId = PUBLISHED_GROUP_ID
                artifactId = ARTIFACT
                version = VERSION

                pom {
                    name.set(LIBRARY_NAME)
                    description.set(LIBRARY_DESC)
                    url.set(GIT_URL)

                    licenses {
                        license {
                            name.set(LICENSE_NAME)
                            url.set(LICENSE_URL)
                        }
                    }

                    developers {
                        developer {
                            id.set(DEVELOPER_ID)
                            name.set(DEVELOPER_NAME)
                            url.set(DEVELOPER_URL)
                        }
                    }

                    scm {
                        url.set(GIT_URL)
                        connection.set(GIT_CONNECTION)
                        developerConnection.set(GIT_DEVELOPER_CONNECTION)
                    }
                }
            }
        }
    }

    tasks.named("signMavenPublication").configure {
        dependsOn("releaseSourcesJar")
    }

    tasks.named("generateMetadataFileForEngageSdkPublication").configure {
        dependsOn("sourcesJar")
    }

    tasks.named("signEngageSdkPublication").configure {
        dependsOn("publishMavenPublicationToMavenLocal")
    }
}