package io.github.kingg22.buildlogic.godot.task

import org.gradle.workers.WorkAction
import java.net.URLClassLoader
import java.util.*

abstract class CodegenRunnerWorkAction : WorkAction<CodegenRunnerWorkParameters> {
    override fun execute() {
        val params: CodegenRunnerWorkParameters = parameters

        // Create URLClassLoader from classpath files
        val classpathFiles = params.classpath.files.toList()
        val urls = classpathFiles.map { it.toURI().toURL() }.toTypedArray()
        val isolatedClassLoader = URLClassLoader(
            urls,
            Thread.currentThread().contextClassLoader,
        )

        // Map plugin-local enum names to codegen enum values via reflection
        val backendEnumClass = isolatedClassLoader.loadClass(
            "io.github.kingg22.godot.codegen.models.config.GeneratorBackend",
        )
        val kindEnumClass = isolatedClassLoader.loadClass(
            "io.github.kingg22.godot.codegen.models.config.GeneratorKind",
        )
        val backendName = params.backendName.get().name
        val kindName = params.kindName.get().name

        // Find enum constant by name
        val backend = backendEnumClass.enumConstants.firstOrNull {
            it.toString().equals(backendName, true)
        } ?: error("Unknown backend: $backendName")
        val kind = kindEnumClass.enumConstants.firstOrNull {
            it.toString().equals(kindName, true)
        } ?: error("Unknown kind: $kindName")

        // Find matching CodegenRunnerProvider via ServiceLoader
        val providerClass = isolatedClassLoader.loadClass(
            "io.github.kingg22.godot.codegen.runner.CodegenRunnerProvider",
        )
        val serviceLoader = ServiceLoader.load(providerClass, isolatedClassLoader)

        val provider = serviceLoader.firstOrNull { isProviderMatch(it, backendName, kindName) }
            ?: error("No CodegenRunnerProvider for backend=$backendName kind=$kindName")

        // Create runner via reflection
        val createRunnerMethod = providerClass.getMethod("createRunner")

        // Get logger from context
        val runner = createRunnerMethod.invoke(provider)

        // Build CodegenConfig via reflection
        val filters = buildFilters(isolatedClassLoader, params)
        val configClass = isolatedClassLoader.loadClass(
            "io.github.kingg22.godot.codegen.models.config.CodegenConfig",
        )
        val config = configClass.constructors.first { it.parameterCount == 9 }.newInstance(
            params.inputInterface.get().asFile,
            params.inputExtension.get().asFile,
            params.outputDir.get().asFile.toPath(),
            params.packageName.get(),
            backend,
            kind,
            params.generateDocs.getOrElse(false),
            params.skipPlatformSpecificApis.getOrElse(false),
            filters,
        )

        // Invoke runner.run(config)
        val runnerInterface = isolatedClassLoader.loadClass(
            "io.github.kingg22.godot.codegen.runner.CodegenRunner",
        )
        val runMethod = runnerInterface.getMethod(
            "run",
            isolatedClassLoader.loadClass("io.github.kingg22.godot.codegen.models.config.CodegenConfig"),
        )
        runMethod.invoke(runner, config)
    }

    private fun isProviderMatch(provider: Any, backendName: String, kindName: String): Boolean = try {
        val b = provider.javaClass.getMethod("getBackend").invoke(provider).toString()
        val k = provider.javaClass.getMethod("getKind").invoke(provider).toString()
        b == backendName && k == kindName
    } catch (_: Exception) {
        false
    }

    private fun buildFilters(classLoader: ClassLoader, params: CodegenRunnerWorkParameters): Any {
        val filtersClass = classLoader.loadClass(
            "io.github.kingg22.godot.codegen.models.config.ApiFilters",
        )
        val allField = filtersClass.getField("ALL")
        val allFilters = allField.get(null)

        val onlyEnums = params.filterOnlyEnums.getOrElse(false)
        val onlyBuiltinClasses = params.filterOnlyBuiltinClasses.getOrElse(false)
        val onlyEngineClasses = params.filterOnlyEngineClasses.getOrElse(false)
        val excludeTypes = params.filterExcludeTypes.get()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        val method = when {
            onlyEnums -> filtersClass.getMethod("onlyEnums")
            onlyBuiltinClasses -> filtersClass.getMethod("onlyBuiltinClasses")
            onlyEngineClasses -> filtersClass.getMethod("onlyEngineClasses")
            excludeTypes.isNotEmpty() -> filtersClass.getMethod("excludingTypes", Set::class.java)
            else -> return allFilters
        }

        return if (method.parameterCount == 0) {
            method.invoke(allFilters)
        } else {
            method.invoke(allFilters, excludeTypes)
        }
    }
}
