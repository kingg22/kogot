import net.ltgt.gradle.errorprone.errorprone

plugins {
    // Apply the java conventions plugin from build-logic.
    id("buildlogic.java-library-conventions")
    id("buildlogic.java-styles-conventions")
    id("buildlogic.java-null-check")
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        disable("InvalidSnippet")
    }
}
