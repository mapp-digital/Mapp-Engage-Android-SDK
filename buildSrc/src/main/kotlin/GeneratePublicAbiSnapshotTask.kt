import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.util.zip.ZipFile
import javax.inject.Inject

abstract class GeneratePublicAbiSnapshotTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val aarFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val outDir = outputDir.get().asFile.apply { mkdirs() }
        val aar = aarFile.get().asFile

        val classesJar = outDir.resolve("classes.jar")
        ZipFile(aar).use { zip ->
            val entry = zip.getEntry("classes.jar")
                ?: throw GradleException("AAR does not contain classes.jar: ${aar.name}")
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

    private fun normalizeAbiDump(value: String): String {
        return value.lineSequence()
            .filterNot { it.trimStart().startsWith("Compiled from ") }
            .joinToString("\n")
            .trim()
            .plus("\n")
    }
}
