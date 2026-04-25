import io.github.kingg22.buildlogic.kotlin.disableJvmAssertions
import io.github.kingg22.buildlogic.kotlin.enableContextParameters

plugins {
    alias(libs.plugins.kotlin.application.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
}

kotlin {
    enableContextParameters()
    disableJvmAssertions()
}

dependencies {
    implementation(projects.codegen.api.kotlinNative)
    implementation(projects.codegen.api.javaFfm)
    implementation(projects.codegen.runtime.kotlinNative)
    implementation(libs.kotlinx.serialization.json)
    // https://github.com/ajalt/clikt/releases
    implementation("com.github.ajalt.clikt:clikt:5.1.0") {
        exclude(group = "com.github.ajalt.mordant")
    }
    // https://github.com/ajalt/mordant/releases
    implementation("com.github.ajalt.mordant:mordant-core:3.0.2")
}

application {
    mainClass.set("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
}

tasks.distTar.configure {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.distZip.configure {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
