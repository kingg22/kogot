import io.github.kingg22.buildlogic.kotlin.disableJvmAssertions

plugins {
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    disableJvmAssertions()
}

dependencies {
    api(projects.codegen.common)
    api(libs.kotlinx.serialization.json)
}
