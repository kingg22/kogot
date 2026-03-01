plugins {
    id("buildlogic.kotlin-application-conventions")
    id("buildlogic.kotlin-styles-conventions")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters")
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinpoet)
}

application {
    mainClass.set("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
}
