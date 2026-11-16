package io.github.kingg22.buildlogic.godot.test

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Loads a Kotlin/Native test library (a GDExtension) into a headless Godot and runs it.
 *
 * The run fails on a failing test, a non-zero Godot exit (crash), a missing runner result marker (the engine died
 * before the tests finished) and on timeout. Output is mirrored to the console and kept in `godot-output.log`.
 */
abstract class GodotTestTask : DefaultTask() {
    /** The linked shared library containing the tests. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val library: RegularFileProperty

    /** The checked-in host project; `bin/` and the `.gdextension` are generated inside it. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val hostProject: DirectoryProperty

    @get:OutputDirectory
    abstract val reportsDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val godotBin: Property<String>

    @get:Input
    @get:Optional
    abstract val godotVersion: Property<String>

    @get:Input
    abstract val timeoutSeconds: Property<Long>

    @TaskAction
    fun run() {
        val binary = GodotBinaryResolver.resolve(godotBin.orNull, godotVersion.orNull)
        val reported = GodotBinaryResolver.reportedVersion(binary)
        godotVersion.orNull?.let { wanted ->
            val minor = wanted.substringBeforeLast('.')
            if (reported?.startsWith(minor) != true) {
                throw GradleException("godot.version=$wanted requested but $binary reports '$reported'")
            }
        }
        logger.lifecycle("godotTest: using $binary ($reported)")

        val host = hostProject.get().asFile
        installLibrary(host)

        importProject(binary, host)

        val reports = reportsDir.get().asFile.apply { mkdirs() }
        val outputLog = File(reports, "godot-output.log")
        val lines = mutableListOf<String>()
        val process = ProcessBuilder(binary.absolutePath, "--headless", "--path", host.absolutePath)
            .redirectErrorStream(true)
            .start()
        val reader = Thread {
            process.inputStream.bufferedReader().forEachLine { line ->
                synchronized(lines) { lines += line }
                // The runner's own protocol lines are noisy; the report below carries them.
                if (!line.startsWith("##teamcity[")) println(line)
            }
        }.apply { isDaemon = true; start() }

        val finished = process.waitFor(timeoutSeconds.get(), TimeUnit.SECONDS)
        if (!finished) process.destroyForcibly().waitFor()
        reader.join(5_000)
        val output = synchronized(lines) { lines.toList() }
        outputLog.writeText(output.joinToString("\n"))

        val run = TestReport.parse(output)
        val problem = diagnose(finished, if (finished) process.exitValue() else null, run)
        TestReport.writeJUnitXml(run, problem, File(reports, "TEST-godotTest.xml"))

        val failed = run.cases.filter { it.failure != null }
        failed.forEach { logger.error("FAILED ${it.suite}.${it.name}: ${it.failure!!.message}\n${it.failure.details}") }
        logger.lifecycle("godotTest: ${run.cases.size} tests, ${failed.size} failed, ${run.cases.count { it.ignored }} ignored")
        if (problem != null || failed.isNotEmpty()) {
            throw GradleException(problem ?: "${failed.size} Godot test(s) failed, see ${reports.resolve("TEST-godotTest.xml")}")
        }
    }

    /** Godot only learns about a GDExtension from `.godot/extension_list.cfg`, which an import pass writes. */
    private fun importProject(binary: File, host: File) {
        val process = ProcessBuilder(binary.absolutePath, "--headless", "--path", host.absolutePath, "--import")
            .redirectErrorStream(true)
            .start()
        val output = StringBuilder()
        val reader = Thread { process.inputStream.bufferedReader().forEachLine { output.appendLine(it) } }
            .apply { isDaemon = true; start() }
        if (!process.waitFor(timeoutSeconds.get(), TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor()
            throw GradleException("Godot --import did not finish within ${timeoutSeconds.get()}s:\n$output")
        }
        reader.join(5_000)
        logger.info("godot --import:\n$output")
    }

    private fun diagnose(finished: Boolean, exit: Int?, run: TestRun): String? {
        val last = run.lastStarted?.let { " (last started test: $it)" }.orEmpty()
        return when {
            !finished -> "Godot did not finish within ${timeoutSeconds.get()}s$last"
            run.runnerExit == null ->
                "Godot exited with code $exit before the tests finished$last: it crashed or aborted, see godot-output.log"
            run.cases.isEmpty() -> "The test runner executed no tests"
            run.runnerExit != 0 && run.cases.none { it.failure != null } ->
                "The test runner exited with code ${run.runnerExit} without a failed test, see godot-output.log"
            else -> null
        }
    }

    private fun installLibrary(host: File) {
        val lib = library.get().asFile
        val binDir = File(host, "bin").apply { mkdirs() }
        lib.copyTo(File(binDir, lib.name), overwrite = true)
        val osFeature = if (lib.extension == "dylib") "macos" else "linux"
        File(host, "kogot_tests.gdextension").writeText(
            """
            [configuration]
            entry_symbol = "kogot_test_init"
            compatibility_minimum = "4.6"

            [libraries]
            $osFeature.debug = "res://bin/${lib.name}"
            """.trimIndent() + "\n",
        )
    }
}
