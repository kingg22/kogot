plugins {
    id("buildlogic.kotlin-application-conventions")
    id("buildlogic.kotlin-styles-conventions")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
            "-Xwhen-expressions=indy",
        )
        // The Kotlin Compiler adds intrinsic assertions which are only relevant
        // when the code is consumed by Java users. Therefore, we can turn this off
        // when code is being consumed by Kotlin users.
    }
}

dependencies {
    implementation(projects.codegen.api.kotlinNative)
    implementation(projects.codegen.api.javaFfm)
    implementation(projects.codegen.runtime.kotlinNative)
    implementation(libs.kotlinx.serialization.json)
    // https://github.com/ajalt/clikt/releases
    implementation("com.github.ajalt.clikt:clikt:5.1.0") {
        exclude(group = "com.github.ajalt.mordant")
    }
    // https://github.com/ajalt/mordant/releases
    implementation("com.github.ajalt.mordant:mordant-core:3.0.2")
}

application {
    mainClass.set("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
}

tasks.distTar.configure {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.distZip.configure {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
