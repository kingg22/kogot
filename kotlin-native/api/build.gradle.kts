plugins {
    id("buildlogic.kotlin-multiplatform-conventions")
    id("buildlogic.kotlin-styles-conventions")
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
