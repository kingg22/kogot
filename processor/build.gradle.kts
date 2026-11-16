plugins {
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet.ksp)
}

// KSP harness: compiles inline Kotlin sources on the JVM and runs the real processor over them.
val godotApiStubDir: Directory = rootProject.layout.projectDirectory.dir("compiler-plugin/testData/api")

dependencies {
    testImplementation(libs.androidx.room.compiler.testing)
    testImplementation(libs.google.truth)
}

tasks.compileTestKotlin {
    compilerOptions.optIn.add("androidx.room3.compiler.processing.ExperimentalProcessingApi")
}

tasks.test {
    // JVM stand-ins for the native-only Godot API, shared with the compiler-plugin tests so they cannot drift.
    inputs.dir(godotApiStubDir).withPropertyName("godotApiStub").withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("kogot.test.godotApiStubDir", godotApiStubDir.asFile.absolutePath)
}
