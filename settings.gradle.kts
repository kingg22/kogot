pluginManagement {
    includeBuild("build-logic")
    includeBuild("codegen")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Use the Foojay Toolchains plugin to automatically download JDKs required by subprojects.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // Use Maven Central as the default repository (where Gradle will download dependencies) in all subprojects.
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "kogot"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// If there are changes in only one of the projects, Gradle will rebuild only the one that has changed.
// Learn more about structuring projects with Gradle - https://docs.gradle.org/8.7/userguide/multi_project_builds.html

/* JVM using FFM API
include(
    "jvm-ffm:ffm",
    "jvm-ffm:api",
    "jvm-ffm:native-register",
    "jvm-ffm:runtime",
)
 */

// Kotlin using Native Cinterop
include(
    "kotlin-native:ffi",
    "kotlin-native:annotations",
    "kotlin-native:api",
    "kotlin-native:binding",
    "kotlin-native:runtime",
    "kotlin-native:sample",
)

// Sample game for SpriteBench
include("mi-juego-prueba:kotlin_native_game:source")
include("mi-juego-prueba:kotlin_native_game:exported")

// processor, and analysis
include(
    "processor",
    "analysis",
)
