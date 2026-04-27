# Godot Codegen System

This document describes the Godot code generation system, including plugin architecture, configuration options, and usage.

## Architecture Overview

```
                    ┌───────────────────────────────────────────┐
                    │   GodotCodegenChorePlugin                 │
                    │   - Global Godot version                  │
                    │   - Precision configuration               │
                    │   - Single source of truth                │
                    └──────────────┬────────────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    ▼
   ┌──────────────────┐  ┌─────────────────┐  ┌─────────────────┐
   │ GodotCodegen     │  │ GodotCodegen    │  │ (Future)        │
   │ KotlinNative     │  │ JavaFfm         │  │ GodotCodegen    │
   │ Plugin           │  │ Plugin          │  │ JniPlugin       │
   └────────┬─────────┘  └────────┬────────┘  └─────────────────┘
            │                     │
            ▼                     ▼
   ┌──────────────────┐  ┌─────────────────┐
   │ kotlin-native:api│  │ jvm-ffm:api     │
   │ (API signatures  │  │                 │
   │  + impl bodies)  │  │                 │
   │ kotlin-native:   │  │                 │
   │ runtime (FFI)    │  │                 │
   └──────────────────┘  └─────────────────┘
```

## Plugins

### GodotCodegenChorePlugin

The **single source of truth** for Godot version and precision configuration.

Applied to the root project. Provides:
- Godot version (e.g., `4.6.1`, `4.6.2`)
- Paths to API files (`extension_api.json`, `gdextension_interface.json`)

#### Configuration Methods

**1. gradle.properties (recommended for version)**
```properties
godotVersion=4.6.1
```

**2. DSL Extension**
```kotlin
// In root build.gradle.kts
godotCodegenChore {
    version = "4.6.1"
    skipPlatformSpecificApis = true
}
```

**3. CLI Arguments**
```bash
./gradlew assemble -PgodotVersion=4.6.1
```

### GodotCodegenConventionsPlugin

Applied to projects that need to generate code. Depends on `GodotCodegenChorePlugin` for configuration.

```kotlin
// In your module's build.gradle.kts
plugins {
    alias(libs.plugins.godot.codegen)
}

tasks.generateGodotExtensionApi.configure {
    backendName.set("kotlin_native")  // or "java_ffm"
    outputKindName.set("api")         // or "runtime"
}
```

## Changing Godot Version

To change the Godot version used for code generation:

1. **Update gradle.properties:**
```properties
godotVersion=4.6.2
```

2. **Ensure the version directory exists:**
```
godot-version/
├── v4_6_1/
│   ├── extension_api.json
│   └── gdextension_interface.json
├── v4_6_2/
│   ├── extension_api.json
│   └── gdextension_interface.json
```

3. **Rebuild:**
```bash
./gradlew clean assemble
```

## CLI Tools

The codegen CLI provides separate entry points for each backend:

### Kotlin Native

```bash
java -jar codegen-cli-knative.jar \
    --input-interface gdextension_interface.json \
    --input-extension extension_api.json \
    --output generated/ \
    --package io.github.kingg22.godot
```

### Java FFM

```bash
java -jar codegen-cli-java-ffm.jar \
    --input-interface gdextension_interface.json \
    --input-extension extension_api.json \
    --output generated/ \
    --package io.github.kingg22.godot
```

### Common CLI Options

| Option | Description |
|--------|-------------|
| `--input-interface` | Path to `gdextension_interface.json` |
| `--input-extension` | Path to `extension_api.json` |
| `--output`, `-o` | Output directory |
| `--package`, `-p` | Base package name |
| `--backend`, `-b` | Backend: `kotlin_native` or `java_ffm` |
| `--kind`, `-k` | Generation kind: `api` or `runtime` |
| `--build-config` | Precision: `float_32`, `float_64`, `double_32`, `double_64` |
| `--skip-platform-specific-apis` | Skip platform-specific APIs |
| `--only-enums` | Generate only enums |
| `--only-builtin-classes` | Generate only builtin classes |
| `--only-engine-classes` | Generate only engine classes |
| `--exclude-types` | Comma-separated types to exclude |

## API Filters

Filters control which types are generated. Use them to reduce build times or generate specific subsets.

### Filter Options

- `--only-enums` - Generate only enum types
- `--only-builtin-classes` - Generate only builtin classes (String, Array, Vector2, etc.)
- `--only-engine-classes` - Generate only engine classes (Node, Resource, etc.)
- `--exclude-types` - Exclude specific types by name

### Examples

**Generate only enums:**
```bash
--only-enums
```

**Generate everything except String and Array:**
```bash
--exclude-types String,Array
```

**Generate only builtin classes:**
```bash
--only-builtin-classes
```

## Gradle Task Configuration

### kotlin-native/api/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform.conventions)
    alias(libs.plugins.godot.codegen)
}

tasks.generateGodotExtensionApi.configure {
    backendName.set("kotlin_native")
    // kind defaults to "api"
}
```

### kotlin-native/runtime/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform.conventions)
    alias(libs.plugins.godot.codegen)
}

tasks.generateGodotExtensionApi.configure {
    backendName.set("kotlin_native")
    outputKindName.set("runtime")
}
```

## Precision Configuration

The `precision` setting controls the size of floating-point types:

| Precision | float size | double size | Pointer size |
|-----------|------------|-------------|--------------|
| `float_32` | 32-bit | 64-bit | 32-bit |
| `float_64` | 64-bit | 64-bit | 64-bit (default) |
| `double_32` | 32-bit | 32-bit | 32-bit |
| `double_64` | 64-bit | 64-bit | 64-bit |

## Generated Output

### API Module

Generates Kotlin wrappers for Godot Engine classes:
- `builtin/` - Builtin types (String, Array, Dictionary, etc.)
- `engine/` - Engine classes (Node, Resource, Sprite2D, etc.)
- `enums/` - Enum types
- `native/` - Native structure bindings

### Runtime Module

Generates FFI bindings for the GDExtension interface:
- `GDExtensionInterface` - Function pointer bindings
- Lazy-loaded singletons for each interface prefix group

## Build Tasks

| Task | Description |
|------|-------------|
| `generateGodotExtensionApi` | Generate Godot API wrappers |
| `assemble` | Build all outputs |
| `check` | Run tests and checks |
| `spotlessApply` | Apply code formatting |

## Troubleshooting

### Version Directory Not Found

```
IllegalStateException: Godot version directory does not exist: .../godot-version/v4_6_3
```

Ensure `godotVersion` in `gradle.properties` matches an existing directory under `godot-version/`.

### Missing extension_api.json

```
IllegalStateException: extension_api.json not found at ...
```

The chore plugin validates paths at configuration time. Ensure the version directory contains all required files.

### IDE Warnings

If you see warnings in the IDE after changing Godot version:
1. Run `./gradlew clean`
2. Regenerate the project with `./gradlew assemble`
3. Restart the IDE

## Future Enhancements

- [ ] Support for additional backends (JNI, etc.)
- [ ] Incremental generation based on API changes
- [ ] Generation of documentation alongside code
- [ ] Validation of API compatibility across versions
