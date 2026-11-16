package io.github.kingg22.kogot.gradle.test

import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * The "test binary" of a `<target>Test` task. The Kotlin Gradle plugin runs it with the arguments it gives any
 * Kotlin/Native test executable (`--ktest_logger=TEAMCITY`, `--ktest_gradle_filter=...`) and turns the TeamCity service
 * messages on its stdout into Gradle test events, which is what the IDE shows.
 *
 * With [GodotTestIsolation.PER_TEST] it lists the tests (`--ktest_list_tests` run inside Godot, honouring the filters),
 * then runs each one in its own headless Godot and re-emits the messages, one suite per class. A test whose Godot died
 * or hung is reported as a failure of that test, with the engine output.
 */
object GodotTestLauncher {
    private const val BEGIN_MARKER = GenerateTestHarnessTask.BEGIN_MARKER
    private const val RESULT_MARKER = GenerateTestHarnessTask.RESULT_MARKER

    @JvmStatic
    fun main(args: Array<String>) {
        val config = Properties().apply { File(args[0]).inputStream().use(::load) }
        val gradleArgs = args.drop(1)
        if (System.getenv("KOGOT_TEST_DEBUG") != null) System.err.println("kogot-test: launcher args=$gradleArgs")
        val host = File(config.getProperty("host"))
        val timeout = config.getProperty("timeout.seconds").toLong()
        val isolation = GodotTestIsolation.valueOf(config.getProperty("isolation"))
        val binary = try {
            GodotBinaryResolver.resolve(config.getProperty("godot.bin"), config.getProperty("godot.version"))
        } catch (e: IllegalStateException) {
            System.err.println("kogot: ${e.message}")
            exitProcess(1)
        }
        config.getProperty("godot.version")?.let { wanted ->
            val reported = GodotBinaryResolver.reportedVersion(binary)
            if (reported?.startsWith(wanted.substringBeforeLast('.')) != true) {
                System.err.println("kogot: Godot $wanted was requested but $binary reports '$reported'")
                exitProcess(1)
            }
        }

        val godot = Godot(binary, host, timeout)
        godot.importProject()
        when (isolation) {
            GodotTestIsolation.NONE -> runAll(godot, gradleArgs)
            GodotTestIsolation.PER_TEST -> runIsolated(godot, gradleArgs)
        }
        System.out.flush()
    }

    /** One Godot for everything: the runner's own messages are forwarded, a missing result is one failed test. */
    private fun runAll(godot: Godot, gradleArgs: List<String>) {
        val run = godot.run(gradleArgs.filterNot { it == "--ktest_no_exit_code" })
        var started: String? = null
        for (line in run.output) {
            when {
                line.startsWith("##teamcity[testStarted") -> started = attr(line, "name")
                line.startsWith("##teamcity[testFinished") -> started = null
            }
            if (!isHarnessLine(line)) println(line)
        }
        if (!run.completed) reportCrash("godot", started ?: "godot", run)
    }

    private fun runIsolated(godot: Godot, gradleArgs: List<String>) {
        val listing = godot.run(listOf("--ktest_list_tests"))
        if (!listing.completed) {
            reportCrash("godot", "listing the tests", listing)
            return
        }
        // `--ktest_list_tests` ignores the filters, so they are applied here, to the listed tests.
        // Several `--tests` patterns arrive as one comma separated value.
        fun patterns(option: String) = gradleArgs.filter { it.startsWith(option) }
            .flatMap { it.substringAfter('=').split(',') }
            .filter(String::isNotBlank)
            .map(::wildcard)
        val positive = patterns("--ktest_gradle_filter=")
        val negative = patterns("--ktest_negative_gradle_filter=")
        val tests = parseListing(listing.output).filter { (suite, test) ->
            fun matches(patterns: List<Regex>) = patterns.any { it.matches("$suite.$test") || it.matches(suite) }
            (positive.isEmpty() || matches(positive)) && !matches(negative)
        }
        var openSuite: String? = null
        for ((suite, test) in tests) {
            if (suite != openSuite) {
                openSuite?.let { println("##teamcity[testSuiteFinished name='${escape(it)}']") }
                println("##teamcity[testSuiteStarted name='${escape(suite)}' locationHint='ktest:suite://${escape(suite)}']")
                openSuite = suite
            }
            runOne(godot, suite, test)
        }
        openSuite?.let { println("##teamcity[testSuiteFinished name='${escape(it)}']") }
    }

    private fun runOne(godot: Godot, suite: String, test: String) {
        val run = godot.run(listOf("--ktest_logger=TEAMCITY", "--ktest_gradle_filter=$suite.$test"))
        var started: String? = null
        for (line in run.output) {
            when {
                line.startsWith("##teamcity[testStarted") -> started = attr(line, "name")
                line.startsWith("##teamcity[testFinished") || line.startsWith("##teamcity[testIgnored") -> started = null
            }
            // The suite is opened once per class by the launcher, not once per process.
            if (!isHarnessLine(line) && !line.startsWith("##teamcity[testSuite")) println(line)
        }
        if (!run.completed) reportCrash(suite, started ?: test, run, startedAlready = started != null)
    }

    private fun reportCrash(suite: String, test: String, run: Godot.Run, startedAlready: Boolean = false) {
        val why = when {
            run.timedOut -> "Godot did not finish within ${run.timeoutSeconds}s and was killed"
            else -> "Godot exited with code ${run.exitCode} before the test finished (crash or abort)"
        }
        val details = run.output.takeLast(40).joinToString("\n")
        if (!startedAlready) println("##teamcity[testStarted name='${escape(test)}' locationHint='ktest:test://$suite.${escape(test)}']")
        println("##teamcity[testFailed name='${escape(test)}' message='${escape(why)}' details='${escape(details)}']")
        println("##teamcity[testFinished name='${escape(test)}' duration='0']")
    }

    /** `--ktest_list_tests` prints `pkg.Class.` followed by `  test` lines. Everything else is engine noise. */
    internal fun parseListing(lines: List<String>): List<Pair<String, String>> {
        val tests = mutableListOf<Pair<String, String>>()
        var suite: String? = null
        for (line in lines) {
            when {
                line.length > 2 && line.startsWith("  ") && !line[2].isWhitespace() -> suite?.let { tests += it to line.trim() }
                line.endsWith(".") && line.none(Char::isWhitespace) -> suite = line.removeSuffix(".")
            }
        }
        return tests
    }

    /** A Kotlin/Native gradle filter: `*` matches anything, the rest is literal. */
    private fun wildcard(pattern: String) = Regex(pattern.split('*').joinToString(".*") { Regex.escape(it) })

    private fun isHarnessLine(line: String) = line == BEGIN_MARKER || line.startsWith(RESULT_MARKER)

    private fun attr(line: String, name: String): String? =
        Regex("""$name='((?:[^'|]|\|.)*)'""").find(line)?.groupValues?.get(1)

    internal fun escape(value: String): String = buildString {
        for (c in value) {
            when (c) {
                '|' -> append("||")
                '\'' -> append("|'")
                '\n' -> append("|n")
                '\r' -> append("|r")
                '[' -> append("|[")
                ']' -> append("|]")
                else -> append(c)
            }
        }
    }

    /** One headless Godot process on the host project; stdout and stderr are merged and kept line by line. */
    private class Godot(val binary: File, val host: File, val timeoutSeconds: Long) {
        class Run(val output: List<String>, val exitCode: Int?, val timedOut: Boolean, val timeoutSeconds: Long) {
            /** The runner reached its end: the result marker was printed. */
            val completed: Boolean = output.any { it.startsWith(RESULT_MARKER) } && !timedOut
        }

        /** Godot only learns about a GDExtension from `.godot/extension_list.cfg`, which an import pass writes. */
        fun importProject() {
            val run = execute(listOf("--import"), withUserArgs = false)
            if (run.timedOut) {
                System.err.println("kogot: godot --import did not finish within ${timeoutSeconds}s")
                exitProcess(1)
            }
        }

        fun run(userArgs: List<String>): Run = execute(userArgs, withUserArgs = true)

        private fun execute(args: List<String>, withUserArgs: Boolean): Run {
            val command = buildList {
                add(binary.absolutePath)
                add("--headless")
                add("--path")
                add(host.absolutePath)
                if (withUserArgs) add("--")
                addAll(args)
            }
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val lines = mutableListOf<String>()
            val reader = Thread { process.inputStream.bufferedReader().forEachLine { synchronized(lines) { lines += it } } }
                .apply { isDaemon = true; start() }
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly().waitFor()
            reader.join(5_000)
            val output = synchronized(lines) { lines.toList() }
            val begin = output.indexOf(BEGIN_MARKER)
            return Run(
                // Drop the engine banner and registration noise that precedes the runner.
                output = if (withUserArgs && begin >= 0) output.drop(begin) else output,
                exitCode = if (finished) process.exitValue() else null,
                timedOut = !finished,
                timeoutSeconds = timeoutSeconds,
            )
        }
    }
}
