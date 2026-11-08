pluginManagement {
    repositories {
        gradlePluginPortal {
            // Kotlin dev builds only live in the bootstrap/dev repos; skip them here.
            content { excludeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
        mavenCentral {
            // Kotlin dev builds only live in the bootstrap/dev repos; skip them here.
            content { excludeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
        // Kotlin bootstrap builds (kogot tracks a 2.5.20-dev compiler for the compiler-plugin-api artifact).
        maven("https://redirector.kotlinlang.org/maven/bootstrap") {
            mavenContent { includeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral {
            // Kotlin dev builds only live in the bootstrap/dev repos; skip them here.
            content { excludeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
        gradlePluginPortal {
            // Kotlin dev builds only live in the bootstrap/dev repos; skip them here.
            content { excludeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
        // Kotlin bootstrap builds (kogot tracks a 2.5.20-dev compiler for the compiler-plugin-api artifact).
        maven("https://redirector.kotlinlang.org/maven/bootstrap") {
            mavenContent { includeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
    }

    // Single source of truth for every dependency coordinate in this repo, including
    // gradle-plugin's own (this is a separate composite build, so it must point at it explicitly).
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "kogot-gradle-plugin"

include(":plugin")
include(":settings-plugin")
