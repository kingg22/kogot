import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

// Puedes cambiar esto o leerlo desde gradle.properties
val targetJava: Provider<Int> = providers
    .gradleProperty("jvmToolchainVersion")
    .map(String::toInt)
    .orElse(24)

// ---------- JAVA ----------
plugins.withId("java") {
    extensions.configure(JavaPluginExtension::class.java) {
        toolchain {
            languageVersion.set(
                JavaLanguageVersion.of(targetJava.get()),
            )
        }
    }
}

plugins.withId("java-library") {
    extensions.configure(JavaPluginExtension::class.java) {
        toolchain {
            languageVersion.set(
                JavaLanguageVersion.of(targetJava.get()),
            )
        }
    }
}

// ---------- KOTLIN JVM ----------
plugins.withId("org.jetbrains.kotlin.jvm") {
    extensions.configure(KotlinJvmProjectExtension::class.java) {
        jvmToolchain(targetJava.get())
    }
}

afterEvaluate {
    logger.lifecycle("JVM versions to use: ${targetJava.get()}")
}
