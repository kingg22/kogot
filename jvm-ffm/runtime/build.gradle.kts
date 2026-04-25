plugins {
    alias(libs.plugins.java.library.conventions)
    alias(libs.plugins.java.styles.conventions)
    alias(libs.plugins.java.nullsafety)
    alias(libs.plugins.kotlin.library.conventions)
    alias(libs.plugins.kotlin.styles.conventions)
}

dependencies {
    implementation(projects.jvmFfmFfm)
}
