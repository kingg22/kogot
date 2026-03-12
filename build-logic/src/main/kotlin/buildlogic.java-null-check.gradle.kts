import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
    `java-library`

    // can't use alias libs.plugins... here
    id("net.ltgt.errorprone")
    id("net.ltgt.nullaway")
}

dependencies {
    errorprone("com.uber.nullaway:nullaway:0.13.1")

    // Some source of nullability annotations; JSpecify recommended,
    // but others supported as well.
    api("org.jspecify:jspecify:1.0.0")

    // Required, but disable checks for this plugin
    errorprone("com.google.errorprone:error_prone_core:2.48.0")
}

nullaway {
    // Progressive adoption of null check only class/interfaces/packages with @NullMarked
    onlyNullMarked.set(true)

    // Use jspecify specification https://jspecify.dev/docs/spec/
    // User guide https://jspecify.dev/docs/user-guide/
    jspecifyMode.set(true)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-XDaddTypeAnnotationsToSymbol=true")
    options.errorprone {
        allSuggestionsAsWarnings.set(true)
        // Disable all warnings in generated code with @Generated
        disableWarningsInGeneratedCode.set(true)

        nullaway {
            // Raise errors when null check fails
            error()
            // Configuration options https://github.com/uber/NullAway/wiki/Configuration

            // Generated code with @Generated annotation is not checked
            treatGeneratedAsUnannotated.set(true)
            // For advanced contracts like null->null (first parameter null, return null, otherwise not null)
            checkContracts.set(true)
            // When use java.util.Optional
            checkOptionalEmptiness.set(true)
            exhaustiveOverride.set(true)
            suggestSuppressions.set(true)
        }
    }
}
