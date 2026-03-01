plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.kotlin-styles-conventions")
    id("buildlogic.godot-codegen")
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
}

tasks.spotlessKotlin {
    dependsOn(generateApi)
}
