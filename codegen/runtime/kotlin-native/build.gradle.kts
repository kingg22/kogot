import io.github.kingg22.buildlogic.kotlin.disableJvmAssertions
import io.github.kingg22.buildlogic.kotlin.enableContextParameters

plugins {
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
}

kotlin {
    enableContextParameters()
    disableJvmAssertions()
}

dependencies {
    api(projects.codegen.common.kotlinNative)
    api(projects.codegen.runtime.common)
    api(libs.kotlinpoet)
}
