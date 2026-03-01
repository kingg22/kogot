plugins {
    id("buildlogic.kotlin-multiplatform-conventions")
    id("buildlogic.kotlin-styles-conventions")
    id("buildlogic.godot-codegen")
}

kotlin {
    compilerOptions {
        explicitApi()
    }

    // linux
    linuxX64 {
        binaries {
            sharedLib {
                baseName = "godot-kotlin-api"
            }
        }
    }
}

tasks.generateGodotExtensionApi.configure {
    backendName.set("kotlin_native")
}
