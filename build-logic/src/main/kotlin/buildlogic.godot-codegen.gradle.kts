import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

// Configuration interna para el classpath del generador
val codegenConfig = configurations.register("codegenConfig") {
    description = "Internal configuration for the godot-codegen plugin. MUST NOT BE USED OUTSIDE THIS PLUGIN."
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    codegenConfig(project(":codegen:cli"))
}

// Registrar la task de forma genérica
val generateGodotTask = tasks.register<GenerateGodotTask>("generateGodotExtensionApi") {
    classpath(codegenConfig)

    inputExtension.convention(
        rootProject.layout.projectDirectory
            .file("godot-version/v4_6_2/extension_api.json"),
    )

    inputInterface.convention(
        rootProject.layout.projectDirectory
            .file("godot-version/v4_6_2/gdextension_interface.json"),
    )

    outputDir.convention(layout.buildDirectory.dir("generated/sources/godotApi"))

    packageName.convention("io.github.kingg22.godot")
}

// ---------- KOTLIN JVM ----------
plugins.withId("org.jetbrains.kotlin.jvm") {
    extensions.configure(KotlinJvmProjectExtension::class.java) {
        generateGodotTask {
            skipPlatformSpecificApis.convention(
                providers.gradleProperty("codegen.skipPlatformSpecificApis")
                    .map { it.toBoolean() },
            )
        }
        sourceSets {
            named(SourceSet.MAIN_SOURCE_SET_NAME) {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                generatedKotlin.srcDir(generateGodotTask.map { it.outputDir })
            }
        }
    }
}

// ---------- KOTLIN Multiplatform ----------
plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.configure(KotlinMultiplatformExtension::class.java) {
        val isMultiTargetNativeProvider = project.provider {
            targets.count { it.platformType == KotlinPlatformType.native } > 1
        }
        generateGodotTask {
            skipPlatformSpecificApis.convention(
                providers.gradleProperty("codegen.skipPlatformSpecificApis")
                    .map { it.toBoolean() }
                    .orElse(isMultiTargetNativeProvider),
            )
        }
        sourceSets {
            nativeMain {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                generatedKotlin.srcDir(generateGodotTask.map { it.outputDir })
            }
        }
    }
}
