import io.github.kingg22.buildlogic.kotlin.disableJvmAssertions
import io.github.kingg22.buildlogic.kotlin.enableContextParameters

plugins {
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    enableContextParameters()
    disableJvmAssertions()
}

dependencies {
    api(projects.codegen.common)
    api(projects.codegen.runtime.common)
    api(libs.kotlinx.serialization.json)
}

tasks.test {
    // Expone el root del repo como system property accesible desde los tests
    systemProperty("kogot.repo.root", rootProject.projectDir.absolutePath)
}
