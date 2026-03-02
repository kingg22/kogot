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
