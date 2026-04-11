---
name: tool-index
description: Quick reference for best tool to use per task type in kogot development. Use when exploring code, editing files, building, or debugging. Trigger when: searching for files, running Godot, building Kotlin Native, debugging GDExtension, or needing to know tool priority order.
---

# Tool Index - kogot

Quick reference for best tool to use per task type, ordered by priority.

## Exploration (use first)

| Task                  | Tool                               | Why                              |
|-----------------------|------------------------------------|----------------------------------|
| Find files by name    | `Glob`                             | Fast, project-scoped             |
| Find files by pattern | `mcp__idea__find_files_by_glob`    | IDE-powered, understands project |
| Search by text        | `Grep` or `mcp__idea__search_text` | Content search                   |
| Search by symbol      | `mcp__idea__search_symbol`         | Semantic (classes, methods)      |
| Search by regex       | `mcp__idea__search_regex`          | Complex patterns                 |
| Understand structure  | `mcp__idea__list_directory_tree`   | Directory tree view              |
| Get file problems     | `mcp__idea__get_file_problems`     | IDE inspection on file           |

## Godot Tools

| Task                     | Command                                                 |
|--------------------------|---------------------------------------------------------|
| Run editor               | `godot --editor --path <project>`                       |
| Run headless server      | `godot --headless --path <project>`                     |
| Run headless with script | `godot --headless -s <script.gd>`                       |
| Export project           | `godot --headless --export-release <platform> <output>` |
| Debug with GDB           | `gdb --args godot --editor --path <project>`            |

## Kotlin Native Tools

| Task                  | Tool/Command                                       |
|-----------------------|----------------------------------------------------|
| Compile Kotlin/Native | `./gradlew :kotlin-native:binding:linkReleaseTest` |
| Generate klib         | Kotlin/Native compiler via Gradle                  |
| Inspect klib          | `konan.lldb` or `klib` tool                        |
| cinterop              | Configured in `.def` files in `kotlin-native/ffi/` |

## Debug Tools

| Task                     | Tool                                    |
|--------------------------|-----------------------------------------|
| Debug native crash       | GDB/LLDB attach to Godot process        |
| Debug JVM                | IntelliJ debugger (run configuration)   |
| Kotlin/Native memory     | Use `kotlin.native.internal.GC` debug   |
| Godot GDExtension errors | Check `godot-version/` for API mismatch |

## Build Tools

| Task                | Command                                    |
|---------------------|--------------------------------------------|
| Compile all         | `./gradlew assemble`                       |
| Type check          | `./gradlew assemble` (runs Kotlin compile) |
| Format code         | `./gradlew spotlessApply`                  |
| Clean build         | `./gradlew clean build`                    |
| Run specific module | `./gradlew :kotlin-native:binding:build`   |

## Order of Tool Selection

1. **Explore**: Glob → Grep → mcp__idea__ search tools
2. **Edit**: Read → Edit → Verify (re-read after edit)
3. **Build**: Gradle assemble → spotlessApply → verify
4. **Debug**: IDE debugger for JVM, GDB/LLDB for native

## Context Awareness

- After 10+ messages: re-read files before editing (auto-compaction may have cleared context)
- Files >500 LOC: read in chunks with offset/limit
- Truncated results: re-run with narrower scope
