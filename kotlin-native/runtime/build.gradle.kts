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
        optIn.addAll(
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi",
        )
        freeCompilerArgs.addAll("-Xcontext-sensitive-resolution")
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(libs.jetbrains.annotations)
        api(projects.kotlinNativeFfi)
    }

    applyDefaultHierarchyTemplate()

    // linux
    linuxX64 {
        binaries {
            sharedLib(buildTypes = listOfNativeBuildType) {
                baseName = "godot-kotlin-runtime"
            }
        }
    }
}

tasks.generateGodotExtensionApi.configure {
    backendName.set("kotlin_native")
    outputKindName.set("runtime")
}
