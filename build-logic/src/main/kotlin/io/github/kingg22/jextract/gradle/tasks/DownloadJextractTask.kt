package io.github.kingg22.jextract.gradle.tasks

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Downloads the jextract archive and its SHA-256 checksum file.
 *
 * Extends [Download] from `de.undercouch.download` which provides:
 *  - Caching: skips download when the destination file already exists and
 *    its content matches (ETag / Last-Modified / size checks).
 *  - Progress logging integrated with Gradle's lifecycle.
 *  - Retry logic and timeout handling.
 *  - No interaction with the project's dependency repositories — MavenCentral
 *    and other repositories declared in `settings.gradle.kts` are unaffected.
 *
 * Both the `.tar.gz` archive and its `.tar.gz.sha256` sibling are downloaded
 * in a single task so they land in the same cache directory, and their
 * up-to-date state is checked together.
 */
@CacheableTask
abstract class DownloadJextractTask : Download() {

    /** URL of the `.tar.gz` archive. */
    @get:Input
    abstract val archiveUrl: Property<String>

    /** URL of the `.tar.gz.sha256` checksum file. */
    @get:Input
    abstract val sha256Url: Property<String>

    /** Destination file for the archive. */
    @get:OutputFile
    abstract val archiveFile: RegularFileProperty

    /** Destination file for the checksum. */
    @get:OutputFile
    abstract val sha256File: RegularFileProperty

    init {
        archiveFile.disallowUnsafeRead()
        sha256File.disallowUnsafeRead()
        overwrite(false) // skip if already downloaded
        onlyIfModified(true) // respect Last-Modified / ETag
        quiet(true)
    }

    @TaskAction
    override fun download() {
        archiveFile.get().asFile.parentFile.mkdirs()
        dest(archiveFile.get().asFile.parentFile)

        // Download both files. de.undercouch.download skips each one individually
        // when already present and up to date.
        src(listOf(archiveUrl.get(), sha256Url.get()))

        super.download()
    }

    companion object {
        const val NAME = "downloadJextract"
    }
}
