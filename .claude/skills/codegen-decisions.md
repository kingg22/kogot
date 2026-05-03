---
name: codegen-decisions
description: Decides HOW to implement codegen changes in kogot. Use when adding new generator, changing type resolution, adding new JSON field handling, or modifying KotlinPoet output. Trigger on: any codegen/, TypeResolver, BodyGenerator, or generated API changes.
---

# Codegen Decision Criteria

## Generated vs hand-written?
- Generated: anything derivable from extension_api.json without ambiguity OR repeated code like Function0...22 → goes in codegen/
- Hand-written: anything that requires semantic judgment Godot doesn't encode → goes in kotlin-native/binding/ or runtime/
- NEVER: manual helpers that hide what the JSON already describes

## Where does new logic go?
- New type resolution rule → TypeResolver (but only if context-independent; see polysemy note)
- New body pattern → BodyGenerator or dedicated NativeXxxGenerator
- New FFI symbol → RuntimeFFIGenerator, lazy by default
- New struct field classification → FieldKind sealed interface in KNativeStructureGenerator

## Type resolution polysemy rule
`float` and `int` are context-dependent. NEVER unify in TypeResolver:
- builtin_classes.constructors.arguments → real_t semantics (build-config dependent)
- utility_functions / engine method args → GDScript semantics (float=Double, int=Long)
- builtin_class_member_offsets → physical storage from meta field
  Each generator must apply its own resolution.

## When to add to Context vs a dedicated index?
Add to Context only if needed by ≥2 generators. Otherwise create a ResolvedXxxModel passed to the specific generator.

## Fptr caching pattern (always)
`private val _fooFptr by lazy(LazyThreadSafetyMode.PUBLICATION) { ... }`
Never eager-load. Never in companion object. Always file-scope private val.

## Output verification required
After any codegen change: run generateGodotExtensionApi, spot-check 3 generated files, verify they compile.
