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

    applyDefaultHierarchyTemplate()

    // linux
    linuxX64 {
        val main by compilations.getting
        val godot by main.cinterops.creating {
            packageName = "io.github.kingg22.godot.internal.ffi"
            defFile(layout.projectDirectory.file("nativeInterop/cinterop/godot.def"))
            includeDirs.allHeaders(rootProject.layout.projectDirectory.file("godot-version/v4_6_1/"))
        }
        binaries {
            sharedLib(buildTypes = listOfNativeBuildType) {
                baseName = "godot-kotlin-ffi"
            }
        }
    }
}
