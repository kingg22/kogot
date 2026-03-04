import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    id("buildlogic.kotlin-multiplatform-conventions")
    id("buildlogic.kotlin-styles-conventions")
    id("buildlogic.godot-codegen")
}

val isRelease = hasProperty("releaseMode") || hasProperty("release") || System.getenv("CI") != null

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
        )
        freeCompilerArgs.addAll("-Xcontext-sensitive-resolution")
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(libs.jetbrains.annotations)
    }

    // linux
    linuxX64 {
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
