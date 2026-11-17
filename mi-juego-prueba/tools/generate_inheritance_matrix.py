#!/usr/bin/env python3
"""Generates `InheritanceMatrix.kt`: one `@Godot` subclass per engine class a user can extend from Kotlin.

The matrix documents, and the `InheritanceTest` checks inside Godot, which generated engine classes a `@Godot` class
can inherit through the KSP processor. The output is a test fixture, written already formatted for ktlint/spotless. A class is included when the generated API makes it extensible, which means
`open` with a public `(nativePtr)` constructor. Classes that are `final`, have an `internal` constructor, or are
singletons are left out, because Kotlin already refuses them at compile time. See `docs/inheritance-matrix.md` for
the full classification and for what Godot itself accepts.

Run it from the repository root after building the generated API (`:kotlin-native:api:generated:assemble`):

    python3 mi-juego-prueba/tools/generate_inheritance_matrix.py [godot-version-dir]

The default version directory is `godot-version/v4_7_2`.
"""

import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
GENERATED = os.path.join(
    ROOT, "kotlin-native/api/generated/build/generated/sources/godot/kotlin_native/api",
)
OUTPUT = os.path.join(
    ROOT, "mi-juego-prueba/kotlin_native_game/src/nativeTest/kotlin/inheritance/InheritanceMatrix.kt",
)
PACKAGE = "io.github.kingg22.godot.test.inheritance"
# Prefix of the generated @Godot subclass: `Matrix<EngineClass>`, registered in the engine under that name.
CLASS_PREFIX = "Matrix"
# The longest line ktlint accepts; a longer declaration is wrapped.
MAX_LINE = 120

# Exist only on Android (Java*/JNI*) or Web (JavaScript*): Godot reports "non-existing parent class" elsewhere.
PLATFORM_ONLY = {"JavaClass", "JavaObject", "JNISingleton", "JavaClassWrapper", "JavaScriptBridge", "JavaScriptObject"}

# Editor classes are registered by Godot at the EDITOR level, after kogot registers every @Godot class (SCENE level),
# so none of them can be a parent today (#241). One sample keeps the failure visible without flooding every scene's
# startup log with ~70 identical registration errors.
EDITOR_SAMPLES = {"EditorPlugin"}

CLASS_DECL = re.compile(
    r'@GodotClass\("(?P<godot>[^"]+)"\)\s*\n(?:@[^\n]*\n)*'
    r"public (?P<open>open |abstract )?class (?P<kotlin>\w+)(?P<ctor> internal| private| protected)? ?(?:constructor)?\(",
)


def generated_classes():
    """Godot name -> (Kotlin FQN, open, constructor visibility), read from the generated API sources."""
    found = {}
    for directory, _, files in os.walk(GENERATED):
        for name in files:
            if not name.endswith(".kt"):
                continue
            path = os.path.join(directory, name)
            package = os.path.relpath(directory, GENERATED).replace(os.sep, ".")
            with open(path, encoding="utf-8") as source:
                for match in CLASS_DECL.finditer(source.read()):
                    found[match.group("godot")] = (
                        f"{package}.{match.group('kotlin')}",
                        bool(match.group("open")),
                        (match.group("ctor") or "public").strip(),
                    )
    return found


def main():
    version_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "godot-version/v4_7_2")
    with open(os.path.join(version_dir, "extension_api.json"), encoding="utf-8") as api_file:
        api = json.load(api_file)
    singletons = {singleton["type"] for singleton in api["singletons"]}
    generated = generated_classes()

    parents = []
    for cls in api["classes"]:
        name = cls["name"]
        if name in singletons or name in PLATFORM_ONLY or name not in generated:
            continue
        if cls["api_type"] == "editor" and name not in EDITOR_SAMPLES:
            continue
        _, is_open, ctor = generated[name]
        if is_open and ctor == "public":
            parents.append(name)
    parents.sort()

    renamed = sorted(name for name in parents if generated[name][0].rsplit(".", 1)[1] != name)
    editor = sorted(name for name in parents if name in EDITOR_SAMPLES)

    def kotlin_list(names):
        return "listOf(\n" + "".join(f'    "{name}",\n' for name in names) + ")"

    # Parents are imported by simple name; a name that clashes with another import is written fully qualified.
    fixed_imports = {
        "io.github.kingg22.godot.api.ExperimentalGodotApi",
        "io.github.kingg22.godot.api.annotations.Godot",
        "kotlinx.cinterop.COpaquePointer",
        "kotlinx.cinterop.ExperimentalForeignApi",
    }
    simple_names = {}
    for fqn in fixed_imports:
        simple_names.setdefault(fqn.rsplit(".", 1)[1], []).append(fqn)
    for name in parents:
        fqn = generated[name][0]
        simple_names.setdefault(fqn.rsplit(".", 1)[1], []).append(fqn)
    imports = set(fixed_imports)
    parent_reference = {}
    for name in parents:
        fqn = generated[name][0]
        if len(simple_names[fqn.rsplit(".", 1)[1]]) == 1:
            imports.add(fqn)
            parent_reference[name] = fqn.rsplit(".", 1)[1]
        else:
            parent_reference[name] = fqn

    def declaration(name):
        head = f"class {CLASS_PREFIX}{name}(nativePtr: COpaquePointer) : {parent_reference[name]}(nativePtr)"
        if len(head) <= MAX_LINE:
            return f"@Godot\n{head}\n"
        return (
            f"@Godot\nclass {CLASS_PREFIX}{name}(nativePtr: COpaquePointer) :\n"
            f"    {parent_reference[name]}(nativePtr)\n"
        )

    lines = [
        "// Generated by mi-juego-prueba/tools/generate_inheritance_matrix.py. DO NOT EDIT.",
        "// One @Godot subclass per engine class a user can extend; checked by InheritanceTest.",
        "@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotApi::class)",
        "",
        f"package {PACKAGE}",
        "",
        *[f"import {fqn}" for fqn in sorted(imports)],
        "",
        f"/** Godot names of the parents in this matrix; each has a `{CLASS_PREFIX}<name>` @Godot subclass below. */",
        f"val INHERITANCE_MATRIX_PARENTS: List<String> = {kotlin_list(parents)}",
        "",
        "/** Parents whose Kotlin class is renamed by the generator: registered with the Kotlin name today (#239). */",
        f"val INHERITANCE_MATRIX_RENAMED: List<String> = {kotlin_list(renamed)}",
        "",
        "/** Editor parents: Godot registers them after kogot registers @Godot classes (#241). */",
        f"val INHERITANCE_MATRIX_EDITOR: List<String> = {kotlin_list(editor)}",
        "",
        *[declaration(name) for name in parents],
    ]

    with open(OUTPUT, "w", encoding="utf-8") as out:
        out.write("\n".join(lines))
    print(f"{len(parents)} parents ({len(renamed)} renamed, {len(editor)} editor) -> {os.path.relpath(OUTPUT, ROOT)}")


if __name__ == "__main__":
    main()
