package io.github.kingg22.kogot.compiler.test

import io.github.kingg22.kogot.compiler.KogotCompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractFirPhasedDiagnosticTest
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

/**
 * FIR diagnostic test running the real K2 compiler with the kogot plugin registered.
 *
 * Every test sees the shared Godot API stand-in from `testData/api/` (see [GodotApiSourceProvider]), so test files only
 * contain the usages they check, with expected diagnostics written inline as `<!NAME!>code<!>`.
 */
open class AbstractKogotDiagnosticTest : AbstractFirPhasedDiagnosticTest(FirParser.LightTree) {
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider =
        EnvironmentBasedStandardLibrariesPathProvider

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        defaultDirectives {
            +FirDiagnosticsDirectives.DISABLE_GENERATED_FIR_TAGS
            +JvmEnvironmentConfigurationDirectives.FULL_JDK
            +CodegenTestDirectives.IGNORE_DEXING // Avoids loading R8 from the classpath.
        }
        useConfigurators(::KogotPluginConfigurator)
        useAdditionalSourceProviders(::GodotApiSourceProvider)
    }
}

/** Registers the plugin exactly as `META-INF/services` does in a real compilation. */
private class KogotPluginConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    private val registrar = KogotCompilerPluginRegistrar()

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        with(registrar) { registerExtensions(configuration) }
    }
}

/** Adds the shared Godot API stand-in (`testData/api/`, passed by Gradle) to every test module. */
private class GodotApiSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<TestFile> = apiStubFiles.map { it.toTestFile() }

    private companion object {
        val apiStubFiles: List<File> by lazy {
            val dir = System.getProperty(API_STUB_DIR_PROPERTY)?.let(::File)
                ?: error("System property '$API_STUB_DIR_PROPERTY' is not set (configured by :compiler-plugin:test)")
            dir.listFiles { file -> file.extension == "kt" }.orEmpty().sortedBy(File::getName)
                .ifEmpty { error("No Godot API stub files found in $dir") }
        }
    }
}

const val API_STUB_DIR_PROPERTY: String = "kogot.test.godotApiStubDir"
