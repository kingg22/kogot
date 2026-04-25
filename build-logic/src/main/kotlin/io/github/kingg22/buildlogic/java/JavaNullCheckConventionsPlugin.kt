package io.github.kingg22.buildlogic.java

import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.NullAwayExtension
import net.ltgt.gradle.nullaway.nullaway
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

@Suppress("unused")
class JavaNullCheckConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.plugins) {
            apply("java-library")
            apply("net.ltgt.errorprone")
            apply("net.ltgt.nullaway")
        }

        target.dependencies {
            // Some source of nullability annotations; JSpecify recommended,
            // but others supported as well.
            "api"("org.jspecify:jspecify:1.0.0")

            "errorprone"("com.uber.nullaway:nullaway:0.13.4")

            // Required, but disable checks for this plugin
            "errorprone"("com.google.errorprone:error_prone_core:2.49.0")
        }

        target.extensions.configure<NullAwayExtension> {
            // Progressive adoption of null-check-only class/interfaces/packages with @NullMarked
            onlyNullMarked.set(true)

            // Use jspecify specification https://jspecify.dev/docs/spec/
            // User guide https://jspecify.dev/docs/user-guide/
            jspecifyMode.set(true)
        }

        target.tasks.withType<JavaCompile>().configureEach {
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
    }
}
