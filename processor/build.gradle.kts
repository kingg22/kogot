plugins {
    id("buildlogic.kotlin-application-conventions")
    id("buildlogic.kotlin-styles-conventions")
}

dependencies {
    implementation(libs.kotlin.reflect)
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet.ksp)

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
