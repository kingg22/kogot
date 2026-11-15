#!/usr/bin/env python3
"""Runs probe.gd over a whole parent list, surviving engine crashes.

When Godot crashes on a class, the class is recorded as CRASH, the native class is instantiated alone (native.gd) to
tell an engine crash from an inheritance one, and the run resumes after it. Writes one TSV row per parent:
`parent, registered|unregistered, ok|null|skipped|CRASH, details`.

Environment:
  GODOT   path to the Godot 4.7.2 binary (required)
  PROJ    Godot project that loads the extension under test (matrix.c's, or mi-juego-prueba for kogot)
  LST     parent list, one Godot class name per line
  OUT     output TSV (default results.tsv)
  plus the KX_* variables of probe.gd / matrix.c (KX_PREFIX, KX_SKIP, KX_POSTINIT, KX_LEVEL)
"""

import os
import re
import subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
GODOT = os.environ["GODOT"]
LST = os.environ["LST"]
PROJ = os.environ["PROJ"]
OUT = os.environ.get("OUT", "results.tsv")


def godot(script, env, timeout):
    return subprocess.run(
        [GODOT, "--headless", "--path", PROJ, "-s", os.path.join(HERE, script)],
        env=env, capture_output=True, text=True, timeout=timeout,
    )


def main():
    names = open(LST, encoding="utf-8").read().split()
    env = dict(os.environ, KX_POSTINIT="1", KX_PARENTS=LST)
    results = {}
    start = 0
    while start < len(names):
        env["KX_START"] = str(start)
        run = godot("probe.gd", env, 600)
        for line in run.stdout.splitlines():
            if line.startswith("KX|"):
                _, index, name, registered, instantiated, info = line.split("|")
                results[int(index)] = (name, registered, instantiated, info)
        if "KX-DONE" in run.stdout:
            break
        index, name = re.findall(r"KX-TRY\|(\d+)\|(\w+)", run.stderr)[-1]
        alone = godot("native.gd", dict(env, KX_ONE=name), 120)
        if f"NATIVE|{name}|ok" in alone.stdout:
            native = "native-ok"
        elif "NATIVE|" in alone.stdout:
            native = "native-null"
        else:
            native = f"native-crash({alone.returncode})"
        results[int(index)] = (name, "registered", "CRASH", native)
        print("crash", name, native, flush=True)
        start = int(index) + 1
    with open(OUT, "w", encoding="utf-8") as out:
        for index in sorted(results):
            out.write("\t".join(results[index]) + "\n")
    print("total", len(results))


if __name__ == "__main__":
    main()
