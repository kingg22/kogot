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
    api(projects.codegen.api.common)
    api(projects.codegen.common.kotlinNative)
    implementation(projects.codegen.runtime.kotlinNative)
    api(libs.kotlinpoet)
}
