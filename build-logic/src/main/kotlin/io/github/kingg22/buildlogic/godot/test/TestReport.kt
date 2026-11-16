package io.github.kingg22.buildlogic.godot.test

import java.io.File

/** One finished Kotlin/Native test, as reported by the runner's TeamCity logger. */
data class TestCase(
    val suite: String,
    val name: String,
    val durationMillis: Long,
    val failure: Failure? = null,
    val ignored: Boolean = false,
) {
    data class Failure(val message: String, val details: String)
}

/** Result of one Godot run: the parsed tests plus what the harness itself reported. */
data class TestRun(
    val cases: List<TestCase>,
    /** Exit code printed by the in-process runner (`KOGOT_TEST_RESULT exit=N`), null when it never got that far. */
    val runnerExit: Int?,
    /** The test that had started but not finished when the output ended (crash / hang), if any. */
    val lastStarted: String?,
)

/** Parses the TeamCity service messages of the Kotlin/Native test runner and writes JUnit XML. */
object TestReport {
    private const val RESULT_MARKER = "KOGOT_TEST_RESULT exit="
    private val serviceMessage = Regex("""##teamcity\[(\w+)((?:\s+\w+='(?:[^'|]|\|.)*')*)\s*]""")
    private val attribute = Regex("""(\w+)='((?:[^'|]|\|.)*)'""")

    fun parse(lines: List<String>): TestRun {
        val cases = mutableListOf<TestCase>()
        val suites = ArrayDeque<String>()
        val running = LinkedHashMap<String, MutableMap<String, String>>()
        var runnerExit: Int? = null

        for (line in lines) {
            line.substringAfter(RESULT_MARKER, "").takeIf { it.isNotEmpty() }?.trim()?.toIntOrNull()?.let { runnerExit = it }
            val match = serviceMessage.find(line) ?: continue
            val attrs = attribute.findAll(match.groupValues[2]).associate { it.groupValues[1] to unescape(it.groupValues[2]) }
            val name = attrs["name"].orEmpty()
            when (match.groupValues[1]) {
                "testSuiteStarted" -> suites.addLast(name)
                "testSuiteFinished" -> if (suites.isNotEmpty()) suites.removeLast()
                "testStarted" -> running[name] = mutableMapOf()
                "testFailed" -> running.getOrPut(name) { mutableMapOf() }.apply {
                    put("message", attrs["message"].orEmpty())
                    put("details", attrs["details"].orEmpty())
                }
                // An ignored test is reported on its own, without testStarted / testFinished.
                "testIgnored" -> cases += TestCase(suites.lastOrNull() ?: "godot", name, 0, ignored = true)
                "testFinished" -> {
                    val state = running.remove(name) ?: mutableMapOf()
                    cases += TestCase(
                        suite = suites.lastOrNull() ?: "godot",
                        name = name,
                        durationMillis = attrs["duration"]?.toLongOrNull() ?: 0,
                        failure = state["message"]?.let { TestCase.Failure(it, state["details"].orEmpty()) },
                        ignored = state["ignored"] == "true",
                    )
                }
            }
        }
        return TestRun(cases, runnerExit, running.keys.lastOrNull())
    }

    fun writeJUnitXml(run: TestRun, extraFailure: String?, target: File) {
        val failures = run.cases.count { it.failure != null } + if (extraFailure != null) 1 else 0
        val tests = run.cases.size + if (extraFailure != null) 1 else 0
        target.parentFile.mkdirs()
        target.writeText(
            buildString {
                appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
                appendLine(
                    """<testsuite name="godotTest" tests="$tests" failures="$failures" errors="0" skipped="${run.cases.count { it.ignored }}">""",
                )
                for (case in run.cases) {
                    append("""  <testcase classname="${esc(case.suite)}" name="${esc(case.name)}" time="${case.durationMillis / 1000.0}"""")
                    when {
                        case.failure != null -> {
                            appendLine(">")
                            appendLine("""    <failure message="${esc(case.failure.message)}">${esc(case.failure.details)}</failure>""")
                            appendLine("  </testcase>")
                        }
                        case.ignored -> appendLine("><skipped/></testcase>")
                        else -> appendLine("/>")
                    }
                }
                if (extraFailure != null) {
                    appendLine("""  <testcase classname="godot" name="process"><failure message="${esc(extraFailure)}"/></testcase>""")
                }
                appendLine("</testsuite>")
            },
        )
    }

    private fun unescape(value: String): String = buildString {
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '|' && i + 1 < value.length) {
                append(
                    when (val next = value[i + 1]) {
                        'n' -> '\n'
                        'r' -> '\r'
                        else -> next
                    },
                )
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }

    private fun esc(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
