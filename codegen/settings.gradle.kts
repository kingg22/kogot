pluginManagement {
    includeBuild("../build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }

    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "codegen"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    ":api:common",
    ":api:java-ffm",
    ":api:kotlin-native",
    ":common",
    ":common:kotlin-native",
    ":cli",
    ":runtime:common",
    ":runtime:kotlin-native",
    ":gradle-plugin",
)
