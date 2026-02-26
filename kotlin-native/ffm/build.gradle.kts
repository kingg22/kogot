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

    // Maintain the list of targets in sync with ktor client core
    // https://ktor.io/docs/client-supported-platforms.html
    // https://github.com/ktorio/ktor/blob/main/build-logic/src/main/kotlin/ktorbuild/targets/KtorTargets.kt
    applyDefaultHierarchyTemplate()

    // linux
    linuxX64 {
        val main by compilations.getting
        val godot by main.cinterops.creating {
            packageName = "io.github.kingg22.godot.internal.ffm"
            includeDirs.allHeaders(rootProject.layout.projectDirectory.file("godot-version/v4_6_1/"))
        }
        binaries {
            sharedLib {
                baseName = "godot-kotlin"
            }
        }
    }
}
