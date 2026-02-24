plugins {
    id("buildlogic.kotlin-application-conventions")
    id("buildlogic.kotlin-styles-conventions")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("com.squareup:kotlinpoet:2.2.0")
}

application {
    mainClass.set("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
}

val generateGodotExtensionApi = tasks.register<GenerateGodotTask>("generateGodotExtensionApi") {
    description = "Generate Godot Extension API wrappers"

    inputExtension.convention(
        rootProject.layout.projectDirectory
            .file("godot-java-binding/raw/v4_6_1/extension_api.json"),
    )

    outputDir.convention(
        rootProject.layout.projectDirectory
            .dir("godot-java-api/build/generated/sources/godotApi"),
    )

    packageName.convention("io.github.kingg22.godot.api")
}

val generateGDExtensionInterface = tasks.register<GenerateGodotTask>("generateGDExtensionInterface") {
    description = "Generate GDExtension interface bindings"

    inputInterface.convention(
        rootProject.layout.projectDirectory
            .file("godot-java-binding/raw/v4_6_1/gdextension_interface.json"),
    )

    outputDir.convention(
        rootProject.layout.projectDirectory
            .dir("godot-java-api/build/generated/sources/gdextensionInterface"),
    )

    packageName.convention("io.github.kingg22.godot.gdextension")
}
