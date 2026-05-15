import io.github.kingg22.buildlogic.godot.conventions.CodegenBackend

plugins {
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.godot.codegen.simple)
}

kotlin {
    compilerOptions {
        explicitApi()
    }
}

godotCodegen {
    backend.set(CodegenBackend.JAVA_FFM)
    packageName.set("io.github.kingg22.godot.api")
}

tasks.spotlessKotlin {
    dependsOn(tasks.generateGodot)
}
