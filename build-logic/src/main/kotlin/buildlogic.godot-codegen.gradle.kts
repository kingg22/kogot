import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// Registrar la configuración para el classpath del generador
val codegenConfig = configurations.create("codegenConfig")

dependencies {
    codegenConfig(project(":codegen"))
}

// Registrar la task de forma genérica
val generateGodotTask = tasks.register<GenerateGodotTask>("generateGodotExtensionApi") {
    classpath = codegenConfig

    inputExtension.convention(
        rootProject.layout.projectDirectory
            .file("godot-version/v4_6_1/extension_api.json"),
    )

    outputDir.convention(layout.buildDirectory.dir("generated/sources/godotApi"))

    packageName.convention("io.github.kingg22.godot.api")
}

// ---------- KOTLIN JVM ----------
plugins.withId("org.jetbrains.kotlin.jvm") {
    extensions.configure(KotlinJvmProjectExtension::class.java) {
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
        sourceSets {
            nativeMain {
                // @OptIn(ExperimentalKotlinGradlePluginApi::class)
                kotlin.srcDir(generateGodotTask.map { it.outputDir })
            }
        }
    }
}
