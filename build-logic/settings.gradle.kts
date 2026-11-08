pluginManagement {
    repositories {
        mavenCentral {
            // Kotlin dev builds only live in the bootstrap/dev repos; skip them here.
            content { excludeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
        gradlePluginPortal {
            // Kotlin dev builds only live in the bootstrap/dev repos; skip them here.
            content { excludeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
        maven("https://redirector.kotlinlang.org/maven/bootstrap") {
            mavenContent { includeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
        maven("https://redirector.kotlinlang.org/maven/dev") {
            mavenContent { includeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Reuse version catalog from the main build.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }

    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    // Use Maven Central as the default repository (where Gradle will download dependencies) in all subprojects.
    repositories {
        mavenCentral {
            // Kotlin dev builds only live in the bootstrap/dev repos; skip them here.
            content { excludeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
        gradlePluginPortal {
            // Kotlin dev builds only live in the bootstrap/dev repos; skip them here.
            content { excludeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
        maven("https://redirector.kotlinlang.org/maven/bootstrap") {
            mavenContent { includeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
        maven("https://redirector.kotlinlang.org/maven/dev") {
            mavenContent { includeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", ".*-dev-.*") }
        }
    }
}

rootProject.name = "build-logic"
