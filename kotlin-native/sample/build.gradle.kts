import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    id("buildlogic.kotlin-multiplatform-conventions")
    id("buildlogic.kotlin-styles-conventions")
}

val isRelease = hasProperty("releaseMode") || hasProperty("release")
val isCi = System.getenv("CI") != null

val listOfNativeBuildType = if (isRelease) {
    listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)
} else if (isCi) {
    listOf()
} else {
    listOf(NativeBuildType.DEBUG)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlinx.cinterop.ExperimentalForeignApi",
            "kotlin.experimental.ExperimentalNativeApi",
        )
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        implementation(projects.kotlinNativeApi)
        implementation(projects.kotlinNativeAnnotations)
    }

    applyDefaultHierarchyTemplate()

    linuxX64 { applyBinariesExport() }
    // mingwX64 { applyBinariesExport() }
}

fun KotlinNativeTarget.applyBinariesExport(baseName: String = "godot-kotlin-sample") {
    binaries {
        sharedLib(buildTypes = listOfNativeBuildType) {
            this.baseName = baseName
        }
    }
}
