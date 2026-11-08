package io.github.kingg22.kogot.compiler

import io.github.kingg22.kogot.compiler.fir.KogotFirExtensionRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

/** Entry point of the kogot K2 compiler plugin, discovered through `META-INF/services`. */
class KogotCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String get() = PLUGIN_ID
    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(KogotFirExtensionRegistrar())
    }

    companion object {
        const val PLUGIN_ID: String = "io.github.kingg22.kogot.compiler"
    }
}
