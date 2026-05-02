pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://redirector.kotlinlang.org/maven/bootstrap")
        maven("https://redirector.kotlinlang.org/maven/dev")
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
        mavenCentral()
        gradlePluginPortal()
        maven("https://redirector.kotlinlang.org/maven/bootstrap")
        maven("https://redirector.kotlinlang.org/maven/dev")
    }
}

rootProject.name = "build-logic"
