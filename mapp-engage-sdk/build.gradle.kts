import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("com.android.library")
    id("jacoco") // The JaCoCo plugin provides code coverage metrics for Java code via integration with JaCoCo.
    id("kotlin-parcelize")
    id("maven-publish")
    id("io.github.tafilovic.central-portal-publisher")
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
val apiDir = layout.projectDirectory.dir("api")
val generatedApiDir = layout.buildDirectory.dir("generated/api")
val publicAbiBaselineFile = apiDir.file("public-abi-baseline.txt").asFile
val internalPublicSymbolsBaselineFile = apiDir.file("internal-public-symbols-baseline.txt").asFile

fun collectInternalPublicSymbols(): List<String> {
    val declarationRegex = Regex(
        "^(?:public\\s+)?(?:data\\s+class|sealed\\s+class|enum\\s+class|annotation\\s+class|class|interface|object)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b"
    )
    val packageRegex = Regex("^package\\s+([a-zA-Z0-9_.]+)")
    val symbols = mutableListOf<String>()

    fileTree("src/main/java/com/appoxee/internal").matching {
        include("**/*.kt")
    }.files.sortedBy { it.path }.forEach fileLoop@{ file ->
        val lines = file.readLines()
        val pkg = lines.firstNotNullOfOrNull { line ->
            packageRegex.find(line.trim())?.groupValues?.get(1)
        } ?: return@fileLoop

        lines.forEach lineLoop@{ line ->
            if (line.isBlank() || line.first().isWhitespace()) return@lineLoop
            if (line.startsWith("internal ") || line.startsWith("private ")) return@lineLoop
            declarationRegex.find(line)?.groupValues?.get(1)?.let { symbol ->
                symbols += "$pkg.$symbol"
            }
        }
    }

    return symbols.sorted()
}

fun collectCoverageExcludesForInterfacesAndPlainDataClasses(): List<String> {
    val packageRegex = Regex("^package\\s+([a-zA-Z0-9_.]+)")
    val interfaceRegex = Regex(
        "^(?:public\\s+|internal\\s+|private\\s+|protected\\s+)?(?:sealed\\s+)?(?:fun\\s+interface|interface)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b"
    )
    val dataClassRegex = Regex(
        "^(?:public\\s+|internal\\s+|private\\s+|protected\\s+)?data\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)\\b"
    )

    return fileTree("src/main/java").matching {
        include("**/*.kt", "**/*.java")
    }.files.flatMap fileLoop@{ file ->
        val lines = file.readLines()
        val pkg = lines.firstNotNullOfOrNull { line ->
            packageRegex.find(line.trim())?.groupValues?.get(1)
        } ?: return@fileLoop emptyList()
        val packagePath = pkg.replace('.', '/')

        lines.mapIndexedNotNull { index, line ->
            val trimmedLine = line.trim()
            interfaceRegex.find(trimmedLine)?.groupValues?.get(1)
                ?: dataClassRegex.find(trimmedLine)
                    ?.groupValues
                    ?.get(1)
                    ?.takeIf { isPlainDataClass(lines, index) }
        }.flatMap { symbol ->
            listOf(
                "**/$packagePath/$symbol.class",
                "**/$packagePath/$symbol\$*.class"
            )
        }
    }.distinct().sorted()
}

fun isPlainDataClass(lines: List<String>, startIndex: Int): Boolean {
    var parenthesisDepth = 0
    var sawConstructor = false

    for (index in startIndex..lines.lastIndex) {
        val code = lines[index].substringBefore("//")

        code.forEach { char ->
            when (char) {
                '(' -> {
                    sawConstructor = true
                    parenthesisDepth++
                }
                ')' -> parenthesisDepth--
                '{' -> if (sawConstructor && parenthesisDepth == 0) return false
            }
        }

        if (sawConstructor && parenthesisDepth == 0) {
            return true
        }
    }

    return false
}


extensions.configure<LibraryExtension> {
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
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }

    publishing {
        singleVariant("prodRelease") {}
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.withType<Test>().configureEach {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val jacocoProdDebugUnitTestReport by tasks.registering(JacocoReport::class) {
    group = "verification"
    description = "Generate JaCoCo XML and HTML coverage reports for the ProdDebug unit tests."

    dependsOn("testProdDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/databinding/**"
    ) + collectCoverageExcludesForInterfacesAndPlainDataClasses()

    val kotlinClasses = fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/prodDebug/compileProdDebugKotlin/classes")) {
        exclude(fileFilter)
    }
    val javaClasses = fileTree(layout.buildDirectory.dir("intermediates/javac/prodDebug/compileProdDebugJavaWithJavac/classes")) {
        exclude(fileFilter)
    }

    classDirectories.setFrom(kotlinClasses, javaClasses)
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    additionalSourceDirs.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.asFile.get()) {
            include(
                "jacoco/testProdDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/prodDebugUnitTest/testProdDebugUnitTest.exec"
            )
        }
    )
}

val generatePublicAbiSnapshot by tasks.registering(GeneratePublicAbiSnapshotTask::class) {
    group = "verification"
    description = "Generate current ABI dump for supported public API packages."
    aarFile.set(tasks.named("assembleProdRelease").map {
        val aarDir = layout.buildDirectory.dir("outputs/aar").get().asFile
        val file = aarDir.listFiles()?.firstOrNull { it.name.endsWith("prod-release.aar") }
            ?: throw GradleException("Unable to locate prod release AAR in $aarDir")
        layout.projectDirectory.file(file.absolutePath)
    })
    outputDir.set(layout.buildDirectory.dir("generated/api"))
}

val checkPublicAbi by tasks.registering {
    group = "verification"
    description = "Fail build when current supported public ABI differs from committed baseline."
    dependsOn(generatePublicAbiSnapshot)

    doLast {
        val currentFile = generatedApiDir.get().asFile.resolve("public-abi-current.txt")
        if (!publicAbiBaselineFile.exists()) {
            throw GradleException("Missing ABI baseline file: ${publicAbiBaselineFile.path}. Run :mapp-engage-sdk:updatePublicAbiBaseline")
        }
        val current = currentFile.readText()
        val baseline = publicAbiBaselineFile.readText()
        if (current != baseline) {
            throw GradleException(
                "Public ABI has changed. If intentional, run :mapp-engage-sdk:updatePublicAbiBaseline and commit ${publicAbiBaselineFile.path}"
            )
        }
    }
}

val updatePublicAbiBaseline by tasks.registering {
    group = "verification"
    description = "Update committed baseline for supported public ABI dump."
    dependsOn(generatePublicAbiSnapshot)

    doLast {
        val currentFile = generatedApiDir.get().asFile.resolve("public-abi-current.txt")
        publicAbiBaselineFile.parentFile.mkdirs()
        currentFile.copyTo(publicAbiBaselineFile, overwrite = true)
    }
}

val checkInternalPublicSymbols by tasks.registering {
    group = "verification"
    description = "Fail build when new public top-level symbols appear under com.appoxee.internal."

    doLast {
        val outputDir = generatedApiDir.get().asFile.apply { mkdirs() }
        val currentSymbols = collectInternalPublicSymbols()
        val currentFile = outputDir.resolve("internal-public-symbols-current.txt")
        currentFile.writeText(currentSymbols.joinToString(separator = "\n", postfix = "\n"))

        if (!internalPublicSymbolsBaselineFile.exists()) {
            throw GradleException("Missing symbols baseline file: ${internalPublicSymbolsBaselineFile.path}. Run :mapp-engage-sdk:updateInternalPublicSymbolsBaseline")
        }

        val baseline = internalPublicSymbolsBaselineFile.readText()
        val current = currentFile.readText()
        if (baseline != current) {
            throw GradleException(
                "Public symbols under com.appoxee.internal have changed. If intentional, run :mapp-engage-sdk:updateInternalPublicSymbolsBaseline and commit ${internalPublicSymbolsBaselineFile.path}"
            )
        }
    }
}

val updateInternalPublicSymbolsBaseline by tasks.registering {
    group = "verification"
    description = "Update committed baseline for public top-level symbols under com.appoxee.internal."

    doLast {
        internalPublicSymbolsBaselineFile.parentFile.mkdirs()
        val symbols = collectInternalPublicSymbols()
        internalPublicSymbolsBaselineFile.writeText(symbols.joinToString(separator = "\n", postfix = "\n"))
    }
}

tasks.register("checkApiCompatibility") {
    group = "verification"
    description = "Run API compatibility and accidental API growth checks."
    dependsOn(checkPublicAbi, checkInternalPublicSymbols)
}

tasks.named("check").configure {
    dependsOn("checkApiCompatibility")
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
    uploadTimeoutMinutes = 15
}
