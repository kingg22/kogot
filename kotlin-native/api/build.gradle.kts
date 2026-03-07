import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    id("buildlogic.kotlin-multiplatform-conventions")
    id("buildlogic.kotlin-styles-conventions")
    id("buildlogic.godot-codegen")
}

val isRelease = hasProperty("releaseMode") || hasProperty("release")

val listOfNativeBuildType = if (isRelease) {
    listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)
} else {
    listOf(NativeBuildType.DEBUG)
}

kotlin {
    compilerOptions {
        explicitApi()
        optIn.addAll(
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi",
            "io.github.kingg22.godot.api.ExperimentalGodotApi",
            "io.github.kingg22.godot.api.ExperimentalGodotKotlin",
        )
        freeCompilerArgs.addAll("-Xcontext-sensitive-resolution")
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(libs.jetbrains.annotations)
        api(projects.kotlinNativeFfi)
        implementation(projects.kotlinNativeRuntime)
    }

    applyDefaultHierarchyTemplate()

    // linux
    linuxX64 {
        val main by compilations.getting
        val godotNativeStructures by main.cinterops.creating {
            packageName = "io.github.kingg22.godot.api.native"
            defFile(layout.projectDirectory.file("nativeInterop/cinterop/extension_api_native.def"))
            includeDirs.allHeaders(layout.projectDirectory.dir("nativeInterop/cinterop"))
        }
        binaries {
            sharedLib(buildTypes = listOfNativeBuildType) {
                baseName = "godot-kotlin-api"
            }
        }
    }
}

tasks.generateGodotExtensionApi.configure {
    backendName.set("kotlin_native")
}
