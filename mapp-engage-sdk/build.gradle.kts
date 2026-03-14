import com.android.build.api.dsl.LibraryExtension
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.util.zip.ZipFile
import javax.inject.Inject

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
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

fun normalizeAbiDump(value: String): String {
    return value.lineSequence()
        .filterNot { it.trimStart().startsWith("Compiled from ") }
        .joinToString("\n")
        .trim()
        .plus("\n")
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

abstract class GeneratePublicAbiSnapshotTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {
    @get:InputDirectory val buildDir = project.layout.buildDirectory
    @get:OutputDirectory val outputDir = project.layout.buildDirectory.dir("generated/api")

    @TaskAction
    fun run() {
        val outDir = outputDir.get().asFile.apply { mkdirs() }
        val aarFile = project.fileTree("${buildDir.get().asFile}/outputs/aar").matching {
            include("*prod-release.aar")
        }.files.singleOrNull()
            ?: throw GradleException("Unable to locate prod release AAR in ${buildDir.get().asFile}/outputs/aar")

        val classesJar = outDir.resolve("classes.jar")
        ZipFile(aarFile).use { zip ->
            val entry = zip.getEntry("classes.jar")
                ?: throw GradleException("AAR does not contain classes.jar: ${aarFile.name}")
            zip.getInputStream(entry).use { input ->
                classesJar.outputStream().use { output -> input.copyTo(output) }
            }
        }

        val classNames = ZipFile(classesJar).use { zip ->
            zip.entries().asSequence()
                .map { it.name }
                .filter { it.endsWith(".class") }
                .filter {
                    it == "com/appoxee/Appoxee.class" ||
                        (it.startsWith("com/appoxee/shared/") && it.count { char -> char == '/' } == 3)
                }
                .filterNot { it.contains("$") }
                .filterNot { it.endsWith("/BuildConfig.class") || it.endsWith("/R.class") }
                .map { it.removeSuffix(".class").replace('/', '.') }
                .sorted()
                .toList()
        }

        if (classNames.isEmpty()) {
            throw GradleException("No public API classes found under com.appoxee/com.appoxee.shared in classes.jar")
        }

        val javapPath = project.file("${System.getProperty("java.home")}/bin/javap")
        if (!javapPath.exists()) {
            throw GradleException("javap not found at ${javapPath.absolutePath}")
        }

        val dump = buildString {
            classNames.forEach { className ->
                appendLine("### $className")
                val stdout = ByteArrayOutputStream()
                val stderr = ByteArrayOutputStream()
                val result = execOperations.exec {
                    commandLine(
                        javapPath.absolutePath,
                        "-classpath",
                        classesJar.absolutePath,
                        "-public",
                        className
                    )
                    standardOutput = stdout
                    errorOutput = stderr
                    isIgnoreExitValue = true
                }
                if (result.exitValue != 0) {
                    throw GradleException("javap failed for $className: ${stderr.toString(Charsets.UTF_8)}")
                }
                appendLine(stdout.toString(Charsets.UTF_8))
            }
        }

        outDir.resolve("public-abi-current.txt").writeText(normalizeAbiDump(dump))
    }
}

val generatePublicAbiSnapshot by tasks.registering(GeneratePublicAbiSnapshotTask::class) {
    group = "verification"
    description = "Generate current ABI dump for supported public API packages."
    dependsOn("assembleProdRelease")
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
