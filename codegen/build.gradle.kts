plugins {
    id("buildlogic.kotlin-application-conventions")
    id("buildlogic.kotlin-styles-conventions")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
            "-Xtype-enhancement-improvements-strict-mode",
            "-Xwhen-expressions=indy",
        )
        // The Kotlin Compiler adds intrinsic assertions which are only relevant
        // when the code is consumed by Java users. Therefore, we can turn this off
        // when code is being consumed by Kotlin users.
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinpoet)
    // https://github.com/ajalt/clikt/releases
    implementation("com.github.ajalt.clikt:clikt:5.0.1") {
        exclude(group = "com.github.ajalt.mordant")
    }
    // https://github.com/ajalt/mordant/releases
    implementation("com.github.ajalt.mordant:mordant-core:3.0.2")
}

application {
    mainClass.set("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
}

tasks.test {
    // Expone el root del repo como system property accesible desde los tests
    systemProperty("kogot.repo.root", rootProject.projectDir.absolutePath)
}
