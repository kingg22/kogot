import io.github.kingg22.buildlogic.kotlin.disableJvmAssertions

plugins {
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
}

kotlin {
    disableJvmAssertions()
}

dependencies {
    api(libs.kotlinpoet)
    api(libs.slf4j.api)
    implementation(libs.clikt) {
        exclude(group = libs.ajalt.mordant.get().group)
    }
    implementation(libs.ajalt.mordant)
}
