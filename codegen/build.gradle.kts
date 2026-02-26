plugins {
    id("buildlogic.kotlin-application-conventions")
    id("buildlogic.kotlin-styles-conventions")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinpoet)
}

application {
    mainClass.set("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
}

tasks.register<GenerateGodotTask>("generateGodotExtensionApi") {
    description = "Generate Godot Extension API wrappers"

    inputExtension.convention(
        rootProject.layout.projectDirectory
            .file("godot-version/v4_6_1/extension_api.json"),
    )

    outputDir.convention(
        rootProject.layout.buildDirectory
            .dir("generated/sources/godotApi"),
    )

    packageName.convention("io.github.kingg22.godot.api")
}

tasks.register<GenerateGodotTask>("generateGDExtensionInterface") {
    description = "Generate GDExtension interface bindings"

    inputInterface.convention(
        rootProject.layout.projectDirectory
            .file("godot-version/v4_6_1/gdextension_interface.json"),
    )

    outputDir.convention(
        rootProject.layout.buildDirectory
            .dir("generated/sources/gdextensionInterface"),
    )

    packageName.convention("io.github.kingg22.godot.internal.gdextension")
}
