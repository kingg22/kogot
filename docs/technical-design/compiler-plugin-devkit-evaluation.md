# Evaluation: Kotlin Compiler Plugin DevKit for the kogot compiler plugin

Status: **evaluated, not adopted** (Nov 2026). This branch only records the findings.

References:

- DevKit: https://github.com/Kotlin/compiler-plugin-dev-kit (`docs/spec.md`, `docs/migration/`), version `0.0.4-dev-376`
- Template: https://github.com/Kotlin/compiler-plugin-template/tree/devkit
- Real-world user: https://github.com/Mr3zee/Library-API-Watchdog

## TL;DR

The DevKit and `kotlin-compiler-plugin-api` solve the same problem — "a compiler plugin breaks when the compiler
changes" — in opposite ways, and today they don't combine:

| | Today: `kotlin-compiler-plugin-api` | DevKit |
|---|---|---|
| Compiles against | the curated plugin API artifact (one version) | the **full** `kotlin-compiler` of **every** supported version |
| Compatibility strategy | a stable(-ish) API surface | one build per compiler version, chosen at runtime |
| Published artifact | one plain jar | one "multi-release" jar with a copy per version |
| Kotlin versions | wherever the API artifact exists: only `2.5.20-dev-*` today | 2.2.20+ CLI, IntelliJ 2025.2+ |
| IDE support | needs the plugin to load in the IDE compiler (no guarantee) | IntelliJ 2026.3+ loads it with no registry flag |

`kotlin-compiler-plugin-api` exists only in Kotlin dev builds (bootstrap: `2.5.20-dev-2623`; dev: `2.5.20-dev-3047` …
`2.5.20-dev-4179`; **not on Maven Central**). The DevKit maps it as `kotlin-compiler-plugin-api=UNSUPPORTED` in its
`kotlin-modules.txt`, and adds `compileOnly(kotlin("compiler"))` to every target by itself. Adopting the DevKit
therefore means dropping the plugin API artifact, which we don't want. **Revisit when the plugin API ships in a 2.5.x
release and the DevKit supports it.**

## How kogot builds the plugin today

- `:compiler-plugin` is a plain Kotlin/JVM module of the main build (`buildlogic.kotlin-library-conventions`).
- `compileOnly(kotlin-compiler-plugin-api)` (KT-90147). The artifact has no `com.intellij` classes, so diagnostic
  factories use `KtElement::class` instead of `PsiElement` (see `KogotDiagnostics.kt`).
- Because the jar references only the plugin API, **one artifact serves the JVM/metadata and the Kotlin/Native
  compilers**, with no relocated "embeddable" variant (see `KogotCompilerGradlePlugin`).
- Entry point: `KogotCompilerPluginRegistrar : CompilerPluginRegistrar`, found through `META-INF/services`.
- Tests: `kotlin-compiler-internal-test-framework` with a hand-written `AbstractFirPhasedDiagnosticTest` subclass,
  a `generateTests` `JavaExec` task, and the `org.jetbrains.kotlin.test.*` system properties wired by hand. Test data
  lives in `testData/` and every test gets the Godot API stand-in from `testData/api/`.
- The Gradle side: `KogotCompilerGradlePlugin : KotlinCompilerPluginSupportPlugin` with a hand-written
  `getPluginArtifact()` and plugin id.

Single Kotlin version: whatever `kotlin` is in `gradle/libs.versions.toml` (`2.5.20-dev-2623`).

## How the DevKit works

### Each Kotlin version is a KMP target

`pluginDevKit("compiler-plugin")` applies `kotlin("multiplatform")` and registers one JVM-like target per supported
version, named after it (`2.4.20-dev-7885` → `k2420_dev_7885`). `commonMain` compiles against each version's compiler;
where an API differs between versions you write `expect`/`actual` in version groups (`versionHierarchy { splitDev(2,
5) }` → `pre25DevMain`/`post25DevMain`). A breaking compiler change becomes a compile error in one target instead of a
`NoSuchMethodError` at a user's site.

Versions come from `cliVersions(min, max)`, `ideaVersions(minBuild)` (mapped through `ide-mappings.txt`, e.g.
`263.7307.200 → 2.5.0-ij263-56`), `testAgainst("…")`, and `useLatestDev()`/`useLatestBootstrap()` for CI canaries. A
`-dev-` version counts as an IDE version unless `forIde = false`.

### One multi-release jar

The first target is the "surrogate": its jar carries every version's classes under
`META-INF/kotlin/plugin/<plugin-id>/versions/<version>/` and the embedded dependencies under `.../dependencies/`
(this replaces Shadow). The manifest gets `Kotlin-Compiler-Plugin-Multi-Release` plus `Since-Build`/`Until-Build` for
the IDE.

### Generated entry points and runtime loading

You implement `DevKitComponentRegistrar` and `DevKitCLP` (no `pluginId`, no `supportsK2`) and name them in
`pluginDevKit { componentRegistrar = …; commandLineProcessor = … }`. The DevKit generates `<pluginPackage>.PluginInfo`
with the real `CompilerPluginRegistrar`/`CommandLineProcessor` and writes `META-INF/services` itself.

On first use, the entry point reads the running compiler version (`KotlinCompilerVersion`, mapped through
`ide-mappings.txt` inside an IDE), picks the closest compatible `versions/<v>` directory, and loads the plugin in an
isolated `URLClassLoader` whose parent is the compiler's loader. Other plugins aren't visible, and a rebuilt jar is
re-read (hot reload).

In Gradle, an artifact transform (`MultiReleasePluginTransform`) hands each target's compilations a jar flattened for
its own version.

### Tests

- Test bases in `org.jetbrains.kotlin.compiler.plugin.devkit.runners`: `DevKitJvmDiagnosticTest`, `DevKitJvmBoxTest`,
  JS variants, and `Backported*` copies of the internal-test-framework bases that compile against every version.
- `configurePlugin(MyComponentRegistrar())` replaces the hand-written `EnvironmentConfigurator`.
- `generateDevKitTestsWithJUnit5 { testClass<…> { model("diagnostics") } }` in a `generateTests` source set; the
  DevKit runs it and passes the roots as system properties.
- Test data moves to `src/commonTest/data`. Dumps can be classified per version (`foo.2420_dev_7885.fir.txt`).
- Every `<target>Test` task gets the matching stdlib and test-framework jars; `allTests` and `check` run them all.
- `DevKitJvmDiagnosticTest` enables `FIR_DUMP` by default. To keep kogot's test data unchanged, extend
  `BackportedFirPhasedDiagnosticTest` with kogot's own directives instead.

### Gradle support plugin

`pluginDevKit("gradle-plugin")` applies `java-gradle-plugin` and `buildconfig` and generates `PluginInfo` (id,
artifact, companion libraries). `KogotCompilerGradlePlugin` would become `DevKitSupportPlugin(PluginInfo.PLUGIN_INFO)`
without `getCompilerPluginId`/`getPluginArtifact`. It also gives versioned functional tests
(`allVersionedFunctionalTests`). Users of the Gradle plugin need the DevKit EAP repository.

### Build structure it expects

Its settings plugin adds the Kotlin `bootstrap`, `dev` and `intellij-dependencies` repositories and applies
`functional-test-publishing` to **every** project of the build. Both the template and Library-API-Watchdog use a
composite build. For kogot, that means `compiler-plugin/` as its own included build (like `gradle-plugin/`), never the
main build with the Kotlin/Native modules. The main build would resolve `io.github.kingg22.kogot:kogot-compiler-plugin`
through the included build instead of the `dependencySubstitution` in `mi-juego-prueba`.

## Differences that matter for kogot

1. **API surface.** The DevKit compiles against the full compiler. We would lose the guarantee that the plugin only
   touches the curated plugin API, and could again use `PsiElement`-based APIs. The plugin API is the direction
   JetBrains points to for long-term stability. The DevKit is the pragmatic answer while that API doesn't exist in
   releases.
2. **Version range.** The plugin API locks kogot to `2.5.20-dev`. The DevKit could cover 2.4.x, 2.5.x and IntelliJ
   2025.2+ (`252` → `2.2.20-ij252`, `253` → `2.3.20-ij253`, `263` → `2.5.0-ij263`), at the cost of `expect`/`actual`
   wherever FIR checker APIs changed. Context-parameter checkers and `KtDiagnosticFactoryN` signatures are likely
   candidates. Not measured.
3. **Kotlin/Native.** The single plugin-API jar works for the JVM and the Native compiler today. Not verified: whether
   the DevKit's multi-release jar and isolated class loader work inside the Kotlin/Native compiler. The spec and the
   template cover only JVM and JS tests. This must be tested before any adoption, since kogot is Native-first.
4. **IDE.** The DevKit's main win: IntelliJ 2026.3+ loads multi-release plugins out of the box. With the plugin API,
   IDE support depends on the IDE's bundled compiler (`2.5.0-ij263`) being able to load a plugin built for
   `2.5.20-dev`. Not verified either.
5. **Build cost.** One compiler download and one compilation plus test run per version. Watchdog-style ranges mean
   roughly 10 targets.
6. **Maturity.** The DevKit is experimental (EAP repository, `0.0.x-dev` versions, "here be dragons").

## Things checked during the evaluation

- The DevKit EAP repository and plugin markers resolve from CI-like environments: settings, `compiler-plugin` and
  `gradle-plugin` markers for `0.0.4-dev-376`.
- `kotlin-compiler-plugin-api`:
  - Maven Central: no metadata (404).
  - Bootstrap repository: only `2.5.20-dev-2623`.
  - Dev repository: 10 builds, `2.5.20-dev-3047` to `2.5.20-dev-4179`.
- The DevKit's `kotlin-modules.txt` lists `kotlin-compiler-plugin-api=UNSUPPORTED`, so the DevKit would fail an IDE
  target that depends on it.
- No code migration was kept. A partial migration was started (source sets moved to `src/commonMain` etc., a
  `compiler-plugin/settings.gradle.kts` with the DevKit settings plugin) and discarded once the plugin API constraint
  was settled.

## When to revisit

- `kotlin-compiler-plugin-api` is published with a 2.5.x release (Beta/RC/stable), **and**
- the DevKit can compile targets against it instead of `kotlin-compiler` (its module map stops marking it `UNSUPPORTED`,
  or offers a switch), **and**
- the Native compiler is confirmed to load DevKit multi-release jars.

## Migration sketch, for later

1. `compiler-plugin/settings.gradle.kts`:
   - `pluginManagement` includes `../build-logic` for the style conventions.
   - Repositories add the DevKit EAP repository.
   - Apply `kotlin("compiler.plugin.devkit")`.
   - Use the root version catalog.
   - Set `rootProject.name = "kogot-compiler-plugin"`, with group/version matching `KogotConventions`.
2. Root `settings.gradle.kts`: `includeBuild("compiler-plugin")` instead of `include("compiler-plugin")`; remove the
   substitution in `mi-juego-prueba/kotlin_native_game/build.gradle.kts`.
3. `compiler-plugin/build.gradle.kts`:
   - Apply `kotlin("multiplatform")` and `pluginDevKit("compiler-plugin")`.
   - Set `pluginPackage = "io.github.kingg22.kogot.compiler"`.
   - Set `componentRegistrar`, `commandLineProcessor`, `generateTestsClass` and the version range.
   - Wire the `kogot.test.*` system properties on `tasks.withType<Test>()`.
4. Sources: `src/main` → `src/commonMain`, `src/testFixtures` → `src/commonTestFixtures`, `src/test` →
   `src/commonTest`, `testData` → `src/commonTest/data`, `GenerateTests.kt` → `src/generateTests`. Delete
   `META-INF/services`.
5. `KogotCompilerPluginRegistrar` → `KogotComponentRegistrar : DevKitComponentRegistrar`; add an empty
   `KogotCommandLineProcessor : DevKitCLP`.
6. Tests: `AbstractKogotDiagnosticTest : BackportedFirPhasedDiagnosticTest({ configurePlugin(...); defaultDirectives
   {...}; useAdditionalSourceProviders(::GodotApiSourceProvider) })`; the generator uses `generateDevKitTestsWithJUnit5`.
7. Gradle plugin: `KogotCompilerGradlePlugin : DevKitSupportPlugin(PluginInfo.PLUGIN_INFO)`.
8. CI: `:compiler-plugin:test` → `allTests` of the included build; add a `useLatestDev()` canary job.
