---
name: stale-context-guard
description: Checklist to run before editing files after a long conversation. Trigger when: 10+ messages in conversation, about to edit a file not seen in last 5 messages, or about to rename/refactor anything.
---

# Before editing any file

1. Re-read the file. Trust nothing from context.
2. For renames: search separately for direct calls, type-level references, string literals, and re-exports (no single grep catches all).
3. For codegen files: check if the file is generated (build/generated/) — never edit generated output directly.
4. For refactors touching >5 files: declare phases explicitly, get approval before Phase 2.

# Verification gate (mandatory before "done")
- ./gradlew assemble — must pass
- ./gradlew spotlessApply — must pass
- If no type-checker configured: state this explicitly

# Signs of stale context
- You're about to add an import that "should be there"
- You're about to write a function you vaguely remember exists somewhere
- The file you're editing "feels right" without re-reading it
