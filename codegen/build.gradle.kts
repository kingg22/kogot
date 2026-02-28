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

tasks.register<GenerateGodotTask>("generateGodotExtensionApi") {
    mainClass.set("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
    classpath = sourceSets["main"].runtimeClasspath

    inputExtension.convention(
        rootProject.layout.projectDirectory
            .file("godot-version/v4_6_1/extension_api.json"),
    )

    outputDir.convention(layout.buildDirectory.dir("generated/sources/godotApi"))

    backendName.convention("kotlin_native")

    packageName.convention("io.github.kingg22.godot.api")
}
