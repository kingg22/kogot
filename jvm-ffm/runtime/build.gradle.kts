plugins {
    id("buildlogic.java-library-conventions")
    id("buildlogic.java-styles-conventions")
    id("buildlogic.java-null-check")
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.kotlin-styles-conventions")
}

dependencies {
    implementation(projects.jvmFfm.ffm)
}
