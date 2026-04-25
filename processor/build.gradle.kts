plugins {
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.analysis)
    implementation(libs.kotlin.reflect)
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.androidx.room.compiler.testing)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.google.truth)
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileTestKotlin {
    compilerOptions.optIn.add("androidx.room.compiler.processing.ExperimentalProcessingApi")
}
