package io.github.kingg22.buildlogic.dokka

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import java.io.File

private const val GH_REPO_BASE_URL = "https://github.com/kingg22/kogot/tree/gh-pages/"

/**
 * adds source links that lead to this repository, allowing readers
 * to easily find source code for inspected declarations
 * @param localDir the directory containing the source code
 * @param relativeUrl the relative path to the source code from the root of the repository
 */
fun DokkaSourceSetSpec.configureRemoteSource(localDir: File, @Language("http-url-reference") relativeUrl: String) {
    sourceLink {
        localDirectory.set(localDir)
        remoteUrl("$GH_REPO_BASE_URL${relativeUrl.removePrefix("/")}")
        remoteLineSuffix.set("#L")
    }
}

@Suppress("unused")
class DokkaConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.dokka")

        target.extensions.configure<DokkaExtension> {
            dokkaPublications.configureEach {
                outputDirectory.set(target.rootProject.file("docs/api"))
            }
            dokkaSourceSets.configureEach {
                skipEmptyPackages.set(true)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                enableJdkDocumentationLink.set(true)
                enableKotlinStdLibDocumentationLink.set(true)
                suppressGeneratedFiles.set(true) // The codegen files are placed outside build/generated to publish
            }
        }
    }
}
