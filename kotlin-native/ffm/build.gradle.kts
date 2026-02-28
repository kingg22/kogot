plugins {
    id("buildlogic.kotlin-multiplatform-conventions")
    id("buildlogic.kotlin-styles-conventions")
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
            packageName = "io.github.kingg22.godot.internal.ffm"
            defFile(layout.projectDirectory.file("nativeInterop/cinterop/godot.def"))
            includeDirs.allHeaders(rootProject.layout.projectDirectory.file("godot-version/v4_6_1/"))
        }
        binaries {
            sharedLib {
                baseName = "godot-kotlin-ffm"
            }
        }
    }
}
