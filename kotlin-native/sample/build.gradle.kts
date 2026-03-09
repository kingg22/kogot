import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    id("buildlogic.kotlin-multiplatform-conventions")
    id("buildlogic.kotlin-styles-conventions")
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
        implementation(projects.kotlinNativeApi)
    }

    applyDefaultHierarchyTemplate()

    // linux
    linuxX64 {
        binaries {
            sharedLib(buildTypes = listOfNativeBuildType) {
                baseName = "godot-kotlin-sample"
            }
        }
    }
}
