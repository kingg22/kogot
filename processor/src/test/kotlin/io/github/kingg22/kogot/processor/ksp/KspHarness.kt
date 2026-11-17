package io.github.kingg22.kogot.processor.ksp

import androidx.room3.compiler.processing.util.CompilationResultSubject
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room3.compiler.processing.util.compiler.compile
import androidx.room3.compiler.processing.util.runKspProcessorTest
import io.github.kingg22.kogot.processor.KogotProcessor
import java.io.File
import java.nio.file.Files

/**
 * Runs the real [KogotProcessor] through KSP over inline Kotlin [sources], on the JVM.
 *
 * Kogot's API is native only, so every compilation also gets the shared JVM stand-ins of the Godot API
 * (`compiler-plugin/testData/api`, also used by the compiler-plugin tests so the two cannot drift).
 *
 * The generated bindings call the native runtime (cinterop, `internal` binding API), so they cannot compile on the JVM:
 * the compilation is always expected to fail and the assertions are about what the processor reported and emitted.
 * Use [hasNoKogotDiagnostics] where a source must be accepted.
 */
fun runKogotProcessor(vararg sources: String, assertions: CompilationResultSubject.() -> Unit) {
    runKspProcessorTest(
        sources = godotApiStubs + kotlinSources(sources, "Test"),
        symbolProcessorProviders = listOf(KogotProcessor.Provider()),
        onCompilationResult = { subject -> subject.compilationDidFail().assertions() },
    )
}

/**
 * Like [runKogotProcessor], but the [dependency] sources are compiled first (with the Godot API stand-ins) and only
 * their class files reach the processor's classpath, the way a library or another module would. Declarations from
 * [dependency] are then read from compiled metadata and annotations, not from source.
 */
fun runKogotProcessorWithDependency(
    dependency: String,
    vararg sources: String,
    assertions: CompilationResultSubject.() -> Unit,
) {
    val workingDir = Files.createTempDirectory("kogot-ksp-dependency").toFile()
    try {
        val compiled = compile(
            workingDir,
            TestCompilationArguments(sources = godotApiStubs + kotlinSources(arrayOf(dependency), "Dependency")),
        )
        check(compiled.success) { "the dependency sources must compile: ${compiled.diagnostics}" }
        runKspProcessorTest(
            sources = kotlinSources(sources, "Test"),
            classpath = compiled.outputClasspath,
            symbolProcessorProviders = listOf(KogotProcessor.Provider()),
            onCompilationResult = { subject -> subject.compilationDidFail().assertions() },
        )
    } finally {
        workingDir.deleteRecursively()
    }
}

private fun kotlinSources(contents: Array<out String>, prefix: String) = contents.mapIndexed { index, content ->
    Source.kotlin("$prefix$index.kt", content.trimIndent())
}

private val godotApiStubs: List<Source> by lazy {
    val dir = File(requireNotNull(System.getProperty("kogot.test.godotApiStubDir")) { "kogot.test.godotApiStubDir" })
    dir.listFiles { file -> file.extension == "kt" }.orEmpty().sortedBy { it.name }
        .map { Source.kotlin("api/${it.name}", it.readText()) }
}

/** No `KOGOT-xxx` diagnostic was reported, whatever else the JVM compiler says about the generated code. */
fun CompilationResultSubject.hasNoKogotDiagnostics() {
    val output = compilationResult.rawOutput()
    check(!output.contains("KOGOT-")) { "expected no KOGOT diagnostics but got:\n$output" }
}
