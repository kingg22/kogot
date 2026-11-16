package io.github.kingg22.kogot.processor.ksp

import androidx.room3.compiler.processing.util.CompilationResultSubject
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.runKspProcessorTest
import io.github.kingg22.kogot.processor.KogotProcessor
import java.io.File

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
    val files = sources.mapIndexed { index, content ->
        Source.kotlin("Test$index.kt", content.trimIndent())
    }
    runKspProcessorTest(
        sources = godotApiStubs + files,
        kotlincArguments = listOf("-Xskip-metadata-version-check"),
        symbolProcessorProviders = listOf(KogotProcessor.Provider()),
        onCompilationResult = { subject -> subject.compilationDidFail().assertions() },
    )
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
