pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // Kotlin bootstrap builds (kogot tracks a 2.5.20-dev compiler for the compiler-plugin-api artifact).
        maven("https://redirector.kotlinlang.org/maven/bootstrap")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        // Kotlin bootstrap builds (kogot tracks a 2.5.20-dev compiler for the compiler-plugin-api artifact).
        maven("https://redirector.kotlinlang.org/maven/bootstrap")
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
