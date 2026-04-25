plugins {
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.godot.codegen)
}

kotlin {
    compilerOptions {
        explicitApi()
    }
}

val generateApi = tasks.generateGodotExtensionApi

generateApi.configure {
    // backendName.set("jvm_ffm")
    // temporal use stubs
    backendName.set("stubs")
    packageName.set("io.github.kingg22.godot.api")
}

tasks.spotlessKotlin {
    dependsOn(generateApi)
}
