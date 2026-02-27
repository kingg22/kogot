package io.github.kingg22.jextract.gradle.extension

import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import javax.inject.Inject

/**
 * Top-level DSL extension for the jextract Gradle plugin.
 *
 * Example usage:
 * ```kotlin
 * jextract {
 *     version = JextractVersion.VERSION_25          // optional – auto-detected from toolchain
 *
 *     headerFile(file("include/hello.h"))           // single header
 *     headerFiles(layout.projectDirectory.dir("include"))  // all headers in a directory
 *
 *     packageName = "com.example.ffi"
 *     headerClassName = "HelloLib"                  // required when multiple headers
 *     outputDir = layout.buildDirectory.dir("generated/jextract")
 *
 *     libraries.add("GL")                           // -l GL
 *     libraries.add(":libGL.so.1")                  // -l :libGL.so.1
 *     useSystemLoadLibrary = true                   // --use-system-load-library
 *
 *     includeDirs.from("third_party/include")       // -I <dir>
 *
 *     includes {
 *         functions.add("glClear")
 *         constants.add("GL_COLOR_BUFFER_BIT")
 *         structs.add("GLFWwindow")
 *         unions.add("SomeUnion")
 *         typedefs.add("GLuint")
 *         vars.add("errno")
 *     }
 *
 *     // macOS only
 *     frameworkDirs.from("/Applications/Xcode.app/.../SDKs/MacOSX.sdk")
 *     frameworks.add("OpenGL")
 *
 *     macros.put("MY_MACRO", "1")                   // -D MY_MACRO=1
 *
 *     dumpIncludesFile = layout.projectDirectory.file("scripts/includes.txt")
 *     argsFile = layout.projectDirectory.file("scripts/jextract_args.txt")  // @argsfile
 *
 *     symbolsClassName = "MySymbols"               // --symbols-class-name
 * }
 * ```
 */
abstract class JextractExtension @Inject constructor(objects: ObjectFactory, layout: ProjectLayout) {

    // ── Tool version ──────────────────────────────────────────────────────────

    /**
     * The jextract version to download.  Defaults to [JextractVersion.VERSION_25].
     * When left unset the plugin will attempt to pick the best version for the
     * project's Java toolchain.
     */
    val version: Property<JextractVersion> = objects.property(JextractVersion::class.java)
        .convention(JextractVersion.VERSION_25)

    val sourceSet = objects.property(String::class.java)
        .convention(SourceSet.MAIN_SOURCE_SET_NAME)

    // ── Input headers ─────────────────────────────────────────────────────────

    /** One or more header files to pass to jextract. */
    val headerFiles: ConfigurableFileCollection = objects.fileCollection()

    /**
     * One file to pass to jextract.
     *
     * @see [org.gradle.api.Project.file]
     */
    fun headerFile(file: Any?) {
        headerFiles.from(file)
    }

    /**
     * One or more header files to pass to jextract.
     *
     * @see [org.gradle.api.Project.files]
     */
    fun headerFiles(vararg files: Any?) {
        headerFiles.from(*files)
    }

    // ── Core output options ───────────────────────────────────────────────────

    /**
     * `-t / --target-package` — Java package for generated classes.
     * If not set, the unnamed package is used.
     */
    val packageName: Property<String> = objects.property(String::class.java)

    /**
     * `--header-class-name` — Name for the generated top-level class.
     * Mandatory when [headerFiles] contains more than one file.
     */
    val headerClassName: Property<String> = objects.property(String::class.java)

    /**
     * `--output` — Directory where generated Java sources are placed.
     * Defaults to `build/generated/jextract`.
     */
    val outputDir: DirectoryProperty = objects.directoryProperty()
        .convention(layout.buildDirectory.dir("generated/jextract"))

    // ── Library loading options ───────────────────────────────────────────────

    /**
     * `-l / --library` — Libraries that the generated class should load.
     * Each entry may be a simple name (`"GL"`), a path prefixed with `:`
     * (`:libGL.so.1`), or an absolute path (`:(/usr/lib/libGL.so.1`).
     */
    val libraries: ListProperty<String> = objects.listProperty(String::class.java)

    /**
     * `--use-system-load-library` — When true, libraries from [libraries] are
     * loaded via `System.loadLibrary` / `System.load` instead of the default
     * lookup mechanism.
     */
    val useSystemLoadLibrary: Property<Boolean> = objects.property(Boolean::class.java)

    // ── Include search paths ──────────────────────────────────────────────────

    /**
     * `-I / --include-dir` — Directories appended to the C pre-processor include
     * search path.  Searched in order.
     */
    val includeDirs: ConfigurableFileCollection = objects.fileCollection()

    // ── Symbol filters ────────────────────────────────────────────────────────

    /** Configures `--include-*` symbol filters. */
    val includes: IncludesSpec = objects.newInstance(IncludesSpec::class.java)

    /** Configures `--include-*` symbol filters with DSL. */
    fun includes(action: Action<in IncludesSpec>) = action.execute(includes)

    // ── Dump includes ─────────────────────────────────────────────────────────

    /**
     * `--dump-includes` — When set, jextract dumps all included symbols into
     * this file rather than (or before) generating bindings.
     */
    val dumpIncludesFile: RegularFileProperty = objects.fileProperty()

    // ── @argsfile ─────────────────────────────────────────────────────────────

    /**
     * A file whose contents are prepended to the jextract invocation as
     * `@<path>`.  Useful for sharing flag files across projects.
     */
    val argsFile: RegularFileProperty = objects.fileProperty()

    // ── Advanced naming ───────────────────────────────────────────────────────

    /**
     * `--symbols-class-name` — Override the name of the generated root header class.
     */
    val symbolsClassName: Property<String> = objects.property(String::class.java)

    // ── Macro definitions ─────────────────────────────────────────────────────

    /**
     * `-D / --define-macro` — Map of `MACRO → value` pairs.
     * If the value is blank, the macro is defined as `1`.
     */
    val macros: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    // ── macOS framework options ───────────────────────────────────────────────

    /**
     * `-F` (macOS only) — Framework directories.
     */
    val frameworkDirs: ConfigurableFileCollection = objects.fileCollection()

    /**
     * `--framework` (macOS only) — Framework names whose library path will be
     * resolved automatically.
     */
    val frameworks: ListProperty<String> = objects.listProperty(String::class.java)

    // ── Version enum ──────────────────────────────────────────────────────────
    enum class JextractVersion(internal val javaMajor: Int) {
        VERSION_25(25),
        VERSION_22(22),
        VERSION_21(21),
        VERSION_20(20),
        VERSION_19(19),
        ;

        companion object {
            @JvmStatic
            fun fromJavaMajor(major: Int) = entries.firstOrNull { it.javaMajor <= major }
                ?: error("No jextract version available for Java $major or this Gradle plugin doesn't support it yet")
        }
    }
}
