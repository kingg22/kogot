# jextract-gradle-plugin

A Gradle plugin that downloads, verifies, caches, and runs [jextract](https://github.com/openjdk/jextract) to generate Java FFM (Foreign Function & Memory API) bindings from C headers.

## Features

- Downloads jextract from `download.java.net` once and caches it in Gradle's user home (shared across all projects on the machine).
- SHA-256 verification of the downloaded archive.
- Automatic extraction; skips re-extraction when output already exists.
- On macOS: removes `com.apple.quarantine` xattr automatically after extraction.
- Strongly typed DSL covering every jextract CLI option.
- Five tasks: download, extract, generate, dump-includes, version, arbitrary run.

## Apply the plugin

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
      mavenLocal()
      gradlePluginPortal()
    }
}
```

(Currently is not published to Gradle Plugin Portal yet)

```kotlin
// build.gradle.kts
plugins {
    id("buildlogic.jextract")
}
```

## DSL reference

```kotlin
jextract {
    // ── Tool version (optional – defaults to VERSION_25) ───────────────────
    version = JextractExtension.JextractVersion.VERSION_25
    sourceSet = "main" // autowired generated code to javac - defaults to "main"

    // ── Input headers (at least one required for the generate task) ────────
    headerFile(file("include/hello.h"))                    // single file
    headerFiles(layout.projectDirectory.dir("include"))    // all files in a dir
    headerFiles(file("a.h"), file("b.h"))                  // vararg

    // ── Output ─────────────────────────────────────────────────────────────
    packageName = "com.example.ffi"          // -t / --target-package
    headerClassName = "HelloLib"             // --header-class-name (required for multiple headers)
    outputDir = layout.buildDirectory.dir("generated/jextract")  // --output

    // ── Libraries to load ──────────────────────────────────────────────────
    libraries.add("GL")                      // -l GL
    libraries.add(":libGL.so.1")             // -l :libGL.so.1
    libraries.add(":/usr/lib/libGL.so.1")    // -l :/full/path
    useSystemLoadLibrary = true              // --use-system-load-library

    // ── C preprocessor ─────────────────────────────────────────────────────
    includeDirs.from("third_party/include")  // -I <dir>
    macros.put("MY_MACRO", "1")              // -D MY_MACRO=1
    macros.put("EMPTY_MACRO", "")            // -D EMPTY_MACRO  (defined as 1)

    // ── Symbol filters (when set, unlisted symbols are excluded) ───────────
    includes {
        functions.add("glClear")
        functions.add("glViewport")
        constants.add("GL_COLOR_BUFFER_BIT")
        structs.add("GLFWwindow")
        unions.add("SomeUnion")
        typedefs.add("GLuint")
        vars.add("errno")
    }

    // ── Advanced ────────────────────────────────────────────────────────────
    symbolsClassName = "MySymbols"           // --symbols-class-name
    dumpIncludesFile = layout.buildDirectory.file("jextract/dump.txt")  // --dump-includes
    argsFile = layout.projectDirectory.file("scripts/jextract_args.txt") // @argsfile

    // ── macOS-only ──────────────────────────────────────────────────────────
    frameworkDirs.from("/path/to/Frameworks") // -F <dir>
    frameworks.add("OpenGL")                  // --framework <name>
}
```

## Available tasks

| Task                   | Description                                                             |
|------------------------|-------------------------------------------------------------------------|
| `downloadJextract`     | Downloads the jextract archive (skips if already in cache)              |
| `extractJextract`      | Extracts the archive; removes macOS quarantine xattr if needed          |
| `jextract`             | Generates Java FFM sources using the `jextract { }` DSL configuration   |
| `jextractDumpIncludes` | Runs `--dump-includes` to list all available symbols to a file          |
| `jextractVersion`      | Prints the jextract `--version` string                                  |
| `jextractRun`          | Passes arbitrary args to jextract: `--args="--help"`                    |

## Supported platforms

| Platform      | Identifier        |
|---------------|-------------------|
| Linux x64     | `linux-x64`       |
| Linux ARM64   | `linux-aarch64`   |
| macOS x64     | `macos-x64`       |
| macOS ARM64   | `macos-aarch64`   |
| Windows x64   | `windows-x64`     |

## Supported jextract versions

| Enum constant                          | Java version |
|----------------------------------------|--------------|
| `JextractVersion.VERSION_25` (default) | 25           |
| `JextractVersion.VERSION_22`           | 22           |
| `JextractVersion.VERSION_21`           | 21           |
| `JextractVersion.VERSION_20`           | 20           |
| `JextractVersion.VERSION_19`           | 19           |
