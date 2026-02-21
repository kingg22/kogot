plugins {
    id("buildlogic.java-library-conventions")
    id("buildlogic.java-styles-conventions")
    id("buildlogic.java-null-check")
}

dependencies {
    implementation(projects.godotJavaBinding)
}
