package io.github.kingg22.jextract.gradle

import io.github.kingg22.jextract.gradle.extension.JextractExtension
import io.github.kingg22.jextract.gradle.internal.JextractPlatform
import io.github.kingg22.jextract.gradle.internal.JextractRelease
import io.github.kingg22.jextract.gradle.internal.PlatformArtifact
import io.github.kingg22.jextract.gradle.internal.release
import io.github.kingg22.jextract.gradle.tasks.DownloadJextractTask
import io.github.kingg22.jextract.gradle.tasks.ExtractJextractTask
import io.github.kingg22.jextract.gradle.tasks.JextractDumpIncludesTask
import io.github.kingg22.jextract.gradle.tasks.JextractRunTask
import io.github.kingg22.jextract.gradle.tasks.JextractTask
import io.github.kingg22.jextract.gradle.tasks.JextractVersionTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider

/**
 * Gradle plugin that downloads, caches, and runs jextract.
 *
 * ## Pipeline
 *
 * ```text
 * [DownloadJextractTask]   — download and cached in ~/.gradle/caches/jextract/
 *         ↓ archiveFile
 * [ExtractJextractTask]    — extracts tar.gz, wired via Gradle task outputs
 *         ↓ jextractBinary (Provider<RegularFile>)
 * [JextractTask]           — @CacheableTask, generates FFM bindings
 * [JextractDumpIncludesTask]
 * [JextractVersionTask]
 * [JextractRunTask]
 * ```
 *
 * **Why not Ivy repositories?**
 * `de.undercouch.download` solves all of this cleanly: it downloads to a
 * configurable cache dir in Gradle user home (shared across projects, no repo
 * conflicts), provides native Gradle up-to-date checks, SHA-256 verification,
 * and progress logging — without touching dependency resolution at all.
 */
@Suppress("unused")
class JextractPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Apply the download plugin so DownloadAction / Download task type are available.
        project.plugins.apply("de.undercouch.download")

        val ext = project.extensions.create("jextract", JextractExtension::class.java)

        // Platform detection is safe at configuration time — reads only JVM system properties.
        val platform = JextractPlatform.current

        val release = ext.version.map { it.release() }
        val platformArtifact = release.map { r ->
            r.artifacts[platform] ?: error(
                "No jextract artifact for platform '${platform.id}' in release " +
                    "Java ${r.javaVersion}+${r.build}-${r.patch}.\n" +
                    "Available platforms: ${r.artifacts.keys.joinToString { it.id }}",
            )
        }

        registerTasks(project, ext, platformArtifact, release)
        // jextractBinary is wired into all consumer tasks inside registerTasks.
        // The provider chain carries the task dependency automatically — no dependsOn needed.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task registration
    // ─────────────────────────────────────────────────────────────────────────

    private fun registerTasks(
        project: Project,
        ext: JextractExtension,
        platformArtifact: Provider<PlatformArtifact>,
        release: Provider<JextractRelease>,
    ) {
        val group = "jextract"
        val cacheBase = project.gradle.gradleUserHomeDir.resolve("caches/jextract")

        /** Cache location: `~/.gradle/caches/jextract/{version}/{build}/{patch}` */
        fun cacheForArtifact(artifact: PlatformArtifact) = cacheBase
            .resolve("${artifact.javaVersion}/${artifact.build}/${artifact.patch}")

        /** Cache location: `~/.gradle/caches/jextract/{version}/{build}/{patch}/jextract-{version}` */
        fun cacheForRelease(release: JextractRelease) = cacheForArtifact(release.artifacts.values.first())

        // ── 1. Download ───────────────────────────────────────────────────────
        // de.undercouch.download caches to Gradle user home and skips re-downloads
        // when the file already exists (ETag / size / checksum check).
        val downloadTask = project.tasks.register(
            DownloadJextractTask.NAME,
            DownloadJextractTask::class.java,
        ) {
            this.group = group
            description = "Downloads the jextract archive and its SHA-256 checksum"

            archiveUrl.set(platformArtifact.map { it.url })
            sha256Url.set(platformArtifact.map { it.sha256Url })
            archiveFile.set(
                project.layout.file(
                    platformArtifact.map { art ->
                        cacheForArtifact(art)
                            .resolve(art.fileName)
                    },
                ),
            )
            sha256File.set(
                project.layout.file(
                    platformArtifact.map { art ->
                        cacheForArtifact(art)
                            .resolve(art.sha256FileName)
                    },
                ),
            )
        }

        // ── 2. Extract ────────────────────────────────────────────────────────
        val extractTask = project.tasks.register(
            ExtractJextractTask.NAME,
            ExtractJextractTask::class.java,
        ) {
            this.group = group
            description = "Extracts the jextract archive and verifies its SHA-256 checksum"

            archiveFile.set(downloadTask.flatMap { it.archiveFile })
            sha256File.set(downloadTask.flatMap { it.sha256File })
            extractedDirName.set(release.map { it.extractedDirName })
            outputDir.set(project.layout.dir(release.map { r -> cacheForRelease(r) }))
        }

        // Binary path derived lazily from the extract task output — carries task dependency.
        val jextractBinary = extractTask.flatMap { it.jextractBinary }

        // ── 3. Primary generation task ────────────────────────────────────────
        val jextractTask = project.tasks.register(JextractTask.NAME, JextractTask::class.java) {
            this.group = group
            description = "Runs jextract to generate Java FFM bindings from C headers"
            this.jextractBinary.set(jextractBinary)
            headerFiles.from(ext.headerFiles)
            argsFile.set(ext.argsFile)
            packageName.set(ext.packageName)
            headerClassName.set(ext.headerClassName)
            libraries.set(ext.libraries)
            useSystemLoadLibrary.set(ext.useSystemLoadLibrary)
            includeDirs.from(ext.includeDirs)
            macros.set(ext.macros)
            includeFunctions.set(ext.includes.functions)
            includeConstants.set(ext.includes.constants)
            includeStructs.set(ext.includes.structs)
            includeUnions.set(ext.includes.unions)
            includeTypedefs.set(ext.includes.typedefs)
            includeVars.set(ext.includes.vars)
            symbolsClassName.set(ext.symbolsClassName)
            frameworkDirs.from(ext.frameworkDirs)
            frameworks.set(ext.frameworks)
            outputDir.set(ext.outputDir)
            dumpIncludesFile.set(ext.dumpIncludesFile)
        }

        // Link the output of jextract (java files) to java src/main
        project.pluginManager.withPlugin("java") {
            val javaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)
            javaPluginExtension.sourceSets.getByName(ext.sourceSet.get()).java.srcDir(jextractTask.map { it.outputDir })
        }

        // ── 4. Dump-includes ──────────────────────────────────────────────────
        project.tasks.register(JextractDumpIncludesTask.NAME, JextractDumpIncludesTask::class.java) {
            this.group = group
            description = "Runs jextract --dump-includes to list all visible symbols"
            this.jextractBinary.set(jextractBinary)
            headerFiles.from(ext.headerFiles)
            includeDirs.from(ext.includeDirs)
            macros.set(ext.macros)
            frameworkDirs.from(ext.frameworkDirs)
            outputFile.convention(project.layout.buildDirectory.file("jextract/dump-includes.txt"))
        }

        // ── 5. Version ────────────────────────────────────────────────────────
        project.tasks.register(JextractVersionTask.NAME, JextractVersionTask::class.java) {
            this.group = group
            description = "Prints the jextract --version string"
            this.jextractBinary.set(jextractBinary)
        }

        // ── 6. Arbitrary run ──────────────────────────────────────────────────
        project.tasks.register(JextractRunTask.NAME, JextractRunTask::class.java) {
            this.group = group
            description = "Runs jextract with arbitrary args (CLI: --args=\"...\")"
            this.jextractBinary.set(jextractBinary)
        }
    }
}
