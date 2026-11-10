# Graph Report - issue-114-implementation-0671a0  (2026-11-09)

## Corpus Check
- 468 files · ~12,855,177 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3487 nodes · 4582 edges · 525 communities (270 shown, 255 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 193 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cf70f955`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- GodotVersion
- Guía de Compatibilidad Backward & Forward
- models/ClassInfo.kt
- Godot Codegen System
- TypeOverloadGenerator.kt
- EnumMask
- GodotBindingGenerator
- Typed Arrays v2 - Implementación Corregida
- 🧠 Cosas IMPORTANTES que debes considerar
- Property Export Annotations
- roadmap-22feb.md
- ApiFilters
- Godot-Rust/GDExt - Resumen Visual de Arquitectura
- GodotBridge.java
- Plan: Migrating SwiftGodot Patterns to Kogot
- summary-abi.md
- SignalParamFactoryTest
- godot-rust como GDExtension: registro, codegen, init y carga
- gdext-rust.md
- at
- jvm_bridge_runtime.c
- BindingInterop.kt
- KDocFormatterTest
- builtin/Utils.kt
- _copyright
- Kogot Binding Philosophy
- Plan y contexto: adaptación GodotSharp → GDExtension JVM (Java/Kotlin)
- .buildTestContext
- PackageRegistry
- Revisión de Startup/Initialization GDExtension (Godot Java)
- EnumeratorShortenerTest
- VariantTypeTestData
- Export.kt
- External Bindings Reference - kogot
- GodotElementType
- LoggerExt.kt
- GodotFFI
- kogot — Kotlin Native GDExtension Binding for Godot 4.6+
- Resumen del Proyecto: Godot Kotlin/Native Binding - Estado Actual
- generated/src/nativeMain/kotlin/io/github/kingg22/godot/api/utils/Utils.kt
- Plan técnico: registro ClassDB + Script Instances con FFM (Java 25)
- FormatUtils.kt
- _copyright
- Contributing to kogot
- Conversación y notas sobre GodotSharp vs GDExtension JVM
- 🧪 Ejemplo completo
- gdextension-interface-notes.md
- ApiItem
- ScriptInstanceBridge
- 🧷 2. HANDLE (puntero opaco)
- Detalle de Capas
- Inspiración Adicional: XProcessing de Room
- Arquitectura KSP Processor para Kogot Bindings
- ClassDBBridge
- ClassExtractor.kt
- jextract-gradle-plugin
- impl/Shared.kt
- KNativeImplGen.kt
- RuntimeTypeResolver
- StructWrapper.kt
- CONTRIBUTING.md
- Kogot Repo Map
- Bitfield enum
- Context
- NativePackageRegistry
- BindingInitializationCallbacks
- FFMUtils
- ExtractJextractTask
- 🧠 Modificadores de tipo
- Godot Versions Tracking
- variantTypeOf
- _copyright
- _copyright
- Types.kt
- StringConverter.kt
- _copyright
- _copyright
- Auto-registro de clases (Java/Kotlin) — diseño
- EnumeratorShortener
- model/ClassInfo.kt
- KogotProcessor
- AbstractCodegenRunner
- BridgeContext
- KotlinCallable
- DiagnosticMessage
- AnnotationResolver
- Kotlin
- Diseño ideal
- InheritanceTree
- GodotBindingGenerator
- CodegenOptions
- BindingProcAddressHolder.kt
- GodotVersion2
- JextractExtension
- Agent Directives: Mechanical Overrides
- AnalysisContext
- Godot JVM (godot-java) — Resumen del estado actual
- signal-design.md
- KPoetUtils.kt
- GodotEnum.kt
- classCreationInfo6
- StringNameCache
- JextractTask
- KogotOptions.kt
- 🧬 3. ALIAS
- Ejemplo conceptual Kotlin
- Extensibilidad
- Test Suite de Room
- ClassGraph vs KSP (Para Entender Utopia)
- BuiltinClassMemberOffsets.kt
- v4_6_0/gdextension_interface.schema.json
- AccessorKind
- ScreamingSnakeCaseTest
- CodegenRunner
- CallableFactory
- castToInternal
- properties
- ValidationResultImpl
- DokkaConventionsPlugin
- GenerateGodotTask
- DiagnosticRenderer
- v4_6_1/gdextension_interface.schema.json
- Generación de Código con KotlinPoet
- BodyGenerator
- NativeStructureParserTest
- properties
- BuiltinSizes.kt
- ImplementationPackageRegistry
- v4_6_2/gdextension_interface.schema.json
- properties
- SnakeToCamelTest
- SimpleTypeResolver
- v4_6_3/gdextension_interface.schema.json
- properties
- v4_7_0/gdextension_interface.schema.json
- properties
- filterValuesNotNull
- SyncMode
- Tool.kt
- GDExtensionInitializationFunction
- GDExtensionPropertyUsageFlags
- v4_7_1/gdextension_interface.schema.json
- GDExtensionClassMethodFlags
- properties
- GDExtensionClassMethodArgumentMetadata
- GDExtensionCallErrorType
- GDExtensionPropertyHint
- RecordingHooks
- JextractRunTask
- DownloadJextractTask
- JextractDumpIncludesTask
- JextractVersionTask
- deprecated
- KotlinLibraryConventionsPlugin
- KotlinStylesConventionsPlugin
- KotlinApplicationConventionsPlugin
- JvmToolchainConventionsPlugin
- TestingJvmConventionsPlugin
- JavaStylesConventionsPlugin
- CommonStylesConventionsPlugin
- GodotCodegenChorePlugin
- generation/kotlin/Shared.kt
- Sprite
- deprecated
- deprecated
- Godot Kotlin Native C Interop Signatures
- .execute
- deprecated
- NativeStructure.kt
- deprecated
- deprecated
- BridgeLifecycleTest.java
- .shutdownResetsLifecycleState
- return_value
- return_value
- return_value
- Singleton.kt
- return_value
- ResolvedBuiltinConstructor.kt
- return_value
- return_value
- toClassInfo
- .classNameForOrDefault
- resolve
- properties
- properties
- KDocFormatter.kt
- KotlinNativeTypeResolver
- NativeEnumGenerator.kt
- DefaultValueGenerator.kt
- properties
- NativeUtilityFunctionGenerator.kt
- NativeVariantGenerator.kt
- NativeEngineClassGenerator.kt
- properties
- properties
- properties
- registerMethod
- required
- generate
- enum
- required
- enum
- Member.kt
- required
- Arguments.kt
- enum
- Interface
- ResolvedRuntimeInterface.kt
- required
- enum
- CallableTrampolines
- DelicateGodotApi.kt
- ExperimentalForInheritanceGodotApi.kt
- required
- ExperimentalGodotKotlin.kt
- ExperimentalGodotApi.kt
- InternalForInheritanceGodotApi.kt
- MustBeVariant.kt
- UsedFromCodegenGeneratedCode.kt
- enum
- checkGodotError
- Godot.kt
- registerCustomSignal
- NodeVirtualCalls
- NodeVirtualDispatcher
- getInstance
- InternalBinding.kt
- required
- enum
- DefaultVariantTypeResolver
- 🏗️ 4. Top-level factory functions
- type
- type
- asList
- exported/build.gradle.kts
- godotKotlinInit
- rules.md
- experimental_types_4.6.1.md
- type
- type
- type
- type
- NoOpPackageRegistry
- .execute
- track.sh
- ValidationContext.kt
- godotVersionRoot
- 🧱 2. Núcleo del diseño
- 🧩 3. DSL para nombres de parámetros
- registerProperty
- model/AnnotationInfo.kt
- model/TypeInfo.kt
- NativeGlobalConstantGenerator.kt
- gdextension_jextract_includes.sh
- generation/kotlin/Constants.kt
- analysis/build.gradle.kts
- RegisterSignal.kt
- source/build.gradle.kts
- kogot-documentation
- code:bash (./gradlew clean assemble)
- code:bash (java -jar codegen-cli-knative.jar \)
- code:block10 (let s1 = GString::from("hello");)
- code:block11 (let r1: Gd<RefCounted> = create_resource();)
- code:block12 (let node: Gd<Node> = Node::new_alloc();)
- code:block13 (✓ #[repr(C)] garantiza layouts correctos)
- code:block14 (✓ ClassSceneMethodTable::load() valida punteros)
- code:block15 (strict (default):)
- code:block16 (JSON (Godot))
- code:block2 (┌───────────────────────────────────────────────────────────)
- code:block3 (JSON Input (extension_api.json))
- code:json ({)
- code:rust (#[repr(C, align(4))])
- code:block6 (Vector3 @ 0x1000 [12 bytes])
- code:block7 (Usuario escribe:)
- code:block8 (gdext/)
- code:block9 (1. Godot carga la DLL/SO de gdext)
- code:block2 (res://itest.gdextension)
- code:block3 (#[gdextension(entry_symbol = itest_init)])
- code:block4 ([lib])
- code:mermaid (flowchart TD)
- code:java (// Clase de usuario)
- code:kotlin (class Player : GodotNode() {)
- code:kotlin (_myNode.myMethod())
- code:kotlin (@Export var speed = 10)
- code:block4 (1. Class registration)
- code:kotlin (class MyNode : Node() {)
- code:text (// runtime/src/nativeMain/kotlin/io/github/kingg22/godot/ffi)
- code:kotlin (class GDExtensionInterfaceGenerator {)
- code:block2 (*GDExtensionClassCreationInfo — struct, both — Describe cómo)
- code:block3 (GDExtensionCallError — struct, both — Describe errores de ll)
- code:block4 (*GDExtensionVariantType — enum, both — Tipos de Variant — NE)
- code:block5 (*GDExtensionVariantCallFunc — write — Llama método dinámico)
- code:block6 (*GDExtensionObjectMethodBindCallFunc — write — Llamar método)
- code:block7 (*GDExtensionInterfaceClassdbRegisterExtensionClass — write —)
- code:block8 (*GDExtensionScriptInstanceCall — write — Invocar método de s)
- code:block9 (GDExtensionBool — alias, both — bool ABI)
- code:kotlin (// builtin/GodotArray.kt)
- code:json ({)
- code:kotlin (operator fun get(index: Int): T = TODO()  // ❌ Inventé T)
- code:kotlin (// 1. Sin genéricos (otras clases):)
- code:kotlin (// ✅ Compila)
- code:kotlin (val arr: GodotArray<Node> = GodotArray())
- code:kotlin (// ✅ Solo constructors del JSON, NO inventados)
- code:kotlin (class GenericBuiltinInterceptor(private val typeResolver: Ty)
- code:kotlin (private class ArrayGenericConfig(...) : GenericConfig {)
- code:kotlin (class NativeBuiltinClassGenerator(...) {)
- code:kotlin (class TypeAliasGenerator(private val genericInterceptor: Gen)
- code:kotlin (// File: builtin/VariantArray.kt)
- code:kotlin (// En factory:)
- code:kotlin (class KotlinNativeImplGenerator(...) {)
- code:bash (git clone https://github.com/godot-rust/gdext.git)
- code:bash (# Inside Godot source tree)
- code:bash (cd /home/kingg22/VisualStudioCodeProjects/graphics.gd)
- code:bash (git clone https://github.com/TreyAnto/GraphicsGD.git)
- code:bash (cd /home/kingg22/IdeaProjects/SwiftGodot)
- code:bash (git clone https://github.com/migueldeicaza/SwiftGodot.git)
- code:block2 (Godot JSON API → codegen/models/ → KotlinPoetGenerator → kot)
- code:block3 (register() → ClassRegistrationHelpers → GDExtension API → Go)
- code:kotlin (// CustomSignalClass.kt)
- code:kotlin (// ACTUAL: Necesita conversiones explícitas)
- code:kotlin (// Currently returns empty Callable())
- code:kotlin (// ObjectRegistry.kt - identity table como SwiftGodot)
- code:kotlin (// OLD)
- code:kotlin (// CallbackTrampoline.kt)
- code:kotlin (// Utils.kt)
- code:bash (cd ~/IdeaProjects/kogot)
- code:bash (# Test String transparency)
- code:text (variant_call → core)
- code:kotlin (Node.call("method"))
- code:text (Si existe ScriptInstanceInfo4 → usar)
- code:block5 (MAJOR.MINOR.PATCH)
- code:block6 (Godot 4.x.y+)
- code:kotlin (if (version >= 4.3))
- code:kotlin (if (functionExists))
- code:block10 (error[KOGOT-001]: @Export on invalid type)
- code:block11 (┌───────────────────────────────────────────────────────────)
- code:kotlin (@Test)
- code:block16 (room3-compiler-processing/src/main/java/androidx/room3/compi)
- code:kotlin (fun wrap(ksType: KSType, allowPrimitives: Boolean): KspType )
- code:block2 (┌───────────────────────────────────────────────────────────)
- code:block20 (analysis/src/main/kotlin/io/github/kingg22/kogot/analysis/)
- code:block21 (processor/src/main/kotlin/.../processor/bridge/)
- code:block22 (room3-compiler-processing-testing/)
- code:kotlin (// processor/build.gradle.kts)
- code:block4 (analysis/src/main/kotlin/io/github/kingg22/kogot/analysis/)
- code:block5 (processor/src/main/kotlin/io/github/kingg22/kogot/processor/)
- code:block6 (processor/src/main/kotlin/io/github/kingg22/kogot/processor/)
- code:block7 (processor/src/main/kotlin/io/github/kingg22/kogot/processor/)
- code:kotlin (interface Generator {)
- code:block9 (processor/src/main/kotlin/io/github/kingg22/kogot/processor/)
- code:kotlin (@JvmInline)
- code:kotlin (typealias GDExtensionInt = Long)
- code:kotlin (typealias Constructor = (Long, Long) -> Unit)
- code:block19 (Long)
- code:block2 (enum)
- code:block20 (const Type*)
- code:block21 (Type**)
- code:block22 (Long)
- code:kotlin (external fun getGodotVersion(ptr: Long))
- code:kotlin (fun getGodotVersion(): GodotVersion {)
- code:block26 (C primitive     → Kotlin primitive)
- code:block29 (struct GDExtensionCallError)
- code:block3 (C: int32_t)
- code:block31 (alias GDExtensionInt = int64_t)
- code:block34 (StringName)
- code:kotlin (enum class InitializationLevel(val value: Int) {)
- code:block5 (C: uint32_t)
- code:kotlin (object Flags {)
- code:block8 (void* a algo interno de Godot)
- code:block9 (GDExtensionStringNamePtr)
- code:kotlin (fun disconnect(callback: ...))
- code:kotlin (val vidaCambio: Signal1<Int> = signal("vida_cambio", param<I)
- code:kotlin (class Signal0 : Signal {)
- code:kotlin (val vidaCambio = signal<Int>("vida_cambio", param("nueva_vid)
- code:kotlin (fun <P1> signal(name: String, param: ParamDescriptor<P1>): S)
- code:bash (godot --dump-extension-api --headless)
- code:bash (mv extension_api.json extension_api_without_docs.json)
- code:bash (godot --dump-extension-api-with-docs --headless)
- code:bash (godot --dump-gdextension-interface --headless)
- code:bash (godot --dump-gdextension-interface-json --headless)
- code:http (GET https://github.com/godotengine/godot/blob/4.6/core/exten)
- code:block2 (kogot/)
- code:kotlin (// Generated from Godot's JSON API — DO NOT EDIT)
- code:kotlin (@Godot class MyNode(nativePtr: COpaquePointer) : Node(native)
- code:bash (./gradlew assemble)
- BuildConfiguration
- godotKotlinInit
- .buildTestContext
- GDExtensionVariantType
- registerSignalsForCustomSignalClass
- Generic virtual method dispatch (issue #20)
- getInstanceBinding
- Provider
- KotlinMultiplatformConventionsPlugin
- TypeInfo.kt
- ExportMethod.kt
- GodotVirtualMethod.kt
- NativeEngineVirtualCallsGenerator.kt
- KogotDiagnostics
- .createCustomInfo2
- return_value
- KogotPluginFunctionalTest
- KogotConventionsTest
- createKotlinScriptInstance
- openTypes.kt
- Virtual dispatch: engine-class argument nullability (issue #141)
- Virtual dispatch: engine-class return nullability (issue #143)
- properties
- .buildTestContext
- complexExpressions.kt
- Virtual dispatch: void* return support (issue #137)
- Virtual dispatch: typedarray::* return support (issue #139)
- required
- enum
- KogotSettingsPluginFunctionalTest
- .execute
- VariantTypesSyncTest
- typeAliases
- Virtual dispatch: heap-backed builtin return support (issue #135)
- Kotlin as a Godot Script Language (issue #42) — design notes
- type
- GenerateEntryPointTaskTest
- kogot Gradle Plugin
- KotlinScriptRegistry
- NativeTargetPresetTest
- signal
- collections.kt
- SimpleTypeResolver
- Interface
- ._profilingGetAccumulatedData
- inspect-object-repro
- MustBeVariantCallChecker
- MustBeVariantPropertyChecker
- SignalDeclarationCallChecker
- annotated
- GodotNative
- ScriptMemberDescriptors.kt
- Annotations.kt
- testData/api/GodotNative.kt
- extractors.kt
- typedContracts.kt
- GodotClass.kt

## God Nodes (most connected - your core abstractions)
1. `KotlinScriptLanguage` - 65 edges
2. `KotlinScript` - 41 edges
3. `Builtin` - 39 edges
4. `valueArguments()` - 36 edges
5. `Variant` - 34 edges
6. `GodotString` - 32 edges
7. `at()` - 29 edges
8. `Property Export Annotations` - 28 edges
9. `StringName` - 25 edges
10. `_copyright` - 23 edges

## Surprising Connections (you probably didn't know these)
- `typeString()` --references--> `Variant`  [EXTRACTED]
  kotlin-native/api/generated/src/nativeMain/kotlin/io/github/kingg22/godot/api/utils/Utils.kt → compiler-plugin/testData/api/Builtins.kt
- `anyToVariantOrNull()` --calls--> `GodotString`  [INFERRED]
  kotlin-native/api/generated/src/nativeMain/kotlin/io/github/kingg22/godot/api/builtin/internal/ConvertToVariant.kt → compiler-plugin/testData/api/Builtins.kt
- `initialize()` --calls--> `GodotString`  [INFERRED]
  kotlin-native/sample/src/nativeMain/kotlin/io/github/kingg22/godot/sample/Main.kt → compiler-plugin/testData/api/Builtins.kt
- `mustBeVariantSamples()` --calls--> `Vector2`  [INFERRED]
  mi-juego-prueba/kotlin_native_game/src/nativeMain/kotlin/MustBeVariantUsage.kt → compiler-plugin/testData/api/Builtins.kt
- `variantList()` --calls--> `Vector2`  [INFERRED]
  mi-juego-prueba/kotlin_native_game/src/nativeMain/kotlin/MustBeVariantUsage.kt → compiler-plugin/testData/api/Builtins.kt

## Import Cycles
- None detected.

## Communities (525 total, 255 thin omitted)

### Community 0 - "GodotVersion"
Cohesion: 0.05
Nodes (17): EmptyContext(), build(), empty(), EnumConstantResolver, NativeStructureField, NativeStructureParser, ApiEnum, GodotVersion (+9 more)

### Community 2 - "models/ClassInfo.kt"
Cohesion: 0.29
Nodes (3): NativeCallablePackageRegistry, KotlinNativeCallableRunner, FileSpec

### Community 4 - "TypeOverloadGenerator.kt"
Cohesion: 0.09
Nodes (47): BuiltinClass, BuiltinClassConstant, BuiltinClassMember, BuiltinEnum, BuiltinMethod, Constructor, Operator, ClassDescriptor (+39 more)

### Community 5 - "EnumMask"
Cohesion: 0.06
Nodes (22): godotVersionRoot(), GDExtensionInterface, loadExtensionApi(), loadExtensionInterface(), EnumEntries, bootstrap(), BridgeLifecycleTest, Override (+14 more)

### Community 6 - "GodotBindingGenerator"
Cohesion: 0.06
Nodes (27): generateExportBuildScript(), CodeBlock, FileSpec, targetBinariesBlock(), KogotExportSpec, KogotSettingsExtension, Action, KogotSettingsPlugin (+19 more)

### Community 9 - "Property Export Annotations"
Cohesion: 0.05
Nodes (37): Class Annotations, Excluded Annotations, @Export, @ExportCategory, @ExportColorNoAlpha, @ExportCustom, @ExportDir, @ExportEnum (+29 more)

### Community 11 - "ApiFilters"
Cohesion: 0.05
Nodes (10): GodotString, PackedStringArray, KotlinScriptLanguage, COpaquePointer, GodotError, Script, ScriptLanguage, packedStringArrayOf() (+2 more)

### Community 13 - "GodotBridge.java"
Cohesion: 0.07
Nodes (13): Initializer, BridgeLifecycle, Hooks, Logger, bootstrap(), GodotBridge, onEditorDeinit(), onEditorInit() (+5 more)

### Community 18 - "gdext-rust.md"
Cohesion: 0.07
Nodes (28): 10\. Conclusiones, 1\. Introducción a la Arquitectura, 2.1 Origen: extension_api.json, 2.2 Generación de Código en Tiempo de Compilación, 2\. Flujo de Generación de Código, 3.1.1 Estrategia de Precarga de Punteros, 3.1 FFI Layer (godot-ffi), 3.2.1 ¿Cómo se Conocen los Offsets? (+20 more)

### Community 19 - "at"
Cohesion: 0.20
Nodes (34): ByteVar, at(), getBoolean(), getBuiltin(), getByte(), getDouble(), getFloat(), getInt() (+26 more)

### Community 20 - "jvm_bridge_runtime.c"
Cohesion: 0.13
Nodes (35): JNIEnv, GDExtensionBool, GDExtensionClassLibraryPtr, GDExtensionInitialization, GDExtensionInterfaceGetProcAddress, cleanup_jvm(), godot_java_bridge_init(), initialize_jvm() (+27 more)

### Community 21 - "BindingInterop.kt"
Cohesion: 0.11
Nodes (27): CArrayPointer, CPointerVarOf, GDExtensionBoolVar, GDExtensionConstTypePtr, GDExtensionConstTypePtrVar, GDExtensionConstVariantPtrVar, GDExtensionTypePtr, GDExtensionTypePtrVar (+19 more)

### Community 23 - "builtin/Utils.kt"
Cohesion: 0.13
Nodes (17): anyToVariant(), notAnnotated(), toVariant(), Variant, variantOf(), anyToVariant(), anyToVariantOrNull(), from() (+9 more)

### Community 24 - "_copyright"
Cohesion: 0.07
Nodes (27): _copyright, format_version, interface, $schema, /**************************************************************************/, /* a copy of this software and associated documentation files (the        */, /* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,   */, /* Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.                  */ (+19 more)

### Community 25 - "Kogot Binding Philosophy"
Cohesion: 0.10
Nodes (20): 1. Modelo crudo del JSON, 2. Índices de consulta inmutables, 3. Modelos enriquecidos para codegen, `BuiltinRuntime.kt` no debe existir, Capas permitidas, `Context` debe adelgazar, Criterio de aceptación arquitectónica, Kogot Binding Philosophy (+12 more)

### Community 26 - "Plan y contexto: adaptación GodotSharp → GDExtension JVM (Java/Kotlin)"
Cohesion: 0.11
Nodes (18): 1) GDExtension init, 2) ClassDB bridge (Java), 3) Script instance bridge (Java), 4) ScriptLanguage (GDExtension), API pública: estrategia recomendada, Componentes mínimos, Contexto, Datos del proyecto actual (+10 more)

### Community 27 - ".buildTestContext"
Cohesion: 0.21
Nodes (6): ValueType, RuntimeFFIConvenienceGen, ClassName, TypeName, ParsedType, RuntimeTypeResolver

### Community 28 - "PackageRegistry"
Cohesion: 0.26
Nodes (3): JavaFfmPackageRegistry, ClassName, PackageRegistry

### Community 29 - "Revisión de Startup/Initialization GDExtension (Godot Java)"
Cohesion: 0.12
Nodes (16): 1) Fuentes analizadas, 2) Modelo correcto y rápido de initialization (derivado de godot-rust + GDExtension), 3) Hallazgos en tu implementación actual, 4) Correcciones recomendadas, 5) Secuencia propuesta concreta para godot-java, 6) Resumen ejecutivo, A. Estructura por fases (prioridad máxima), B. Hardening C (+8 more)

### Community 31 - "VariantTypeTestData"
Cohesion: 0.33
Nodes (3): ErrorCase, TypeCase, VariantTypeTestData

### Community 32 - "Export.kt"
Cohesion: 0.12
Nodes (15): ExportCategory, ExportCustom, ExportEnum, ExportExpEasing, ExportFile, ExportFilePath, ExportFlags, ExportGlobalFile (+7 more)

### Community 34 - "GodotElementType"
Cohesion: 0.14
Nodes (12): Builder, ExperimentalInfo, ExperimentalTypesRegistry, GodotElementType, CLASS, CONSTANT, ENUM, METHOD (+4 more)

### Community 35 - "LoggerExt.kt"
Cohesion: 0.18
Nodes (3): currentClassLogger(), fileLogger(), logger()

### Community 36 - "GodotFFI"
Cohesion: 0.28
Nodes (3): GodotFFI, Arena, MemorySegment

### Community 39 - "generated/src/nativeMain/kotlin/io/github/kingg22/godot/api/utils/Utils.kt"
Cohesion: 0.14
Nodes (5): errorString(), errorStringAsGdStr(), GodotError, typeString(), typeStringAsGdStr()

### Community 41 - "FormatUtils.kt"
Cohesion: 0.50
Nodes (3): JavaLibraryConventionsPlugin, Plugin, Project

### Community 42 - "_copyright"
Cohesion: 0.07
Nodes (27): _copyright, format_version, interface, $schema, /**************************************************************************/, /* a copy of this software and associated documentation files (the        */, /* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,   */, /* Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.                  */ (+19 more)

### Community 44 - "Conversación y notas sobre GodotSharp vs GDExtension JVM"
Cohesion: 0.15
Nodes (12): Archivos clave referenciados, Causa probable, Conversación y notas sobre GodotSharp vs GDExtension JVM, Diferencias a tener en cuenta (GodotSharp vs JVM), Estado actual del proyecto JVM, Implicaciones para JVM + GDExtension, Objetivo del usuario, Problema reportado (+4 more)

### Community 47 - "ApiItem"
Cohesion: 0.05
Nodes (23): CliktCommand, CodegenCommand, main(), ApiFilters, Builder, ApiItem, BuiltinClass, EngineClass (+15 more)

### Community 48 - "ScriptInstanceBridge"
Cohesion: 0.24
Nodes (7): FunctionalInterface, Arena, MemorySegment, Override, ScriptHandle, ScriptInstance, ScriptInstanceBridge

### Community 53 - "ClassDBBridge"
Cohesion: 0.27
Nodes (9): ClassDBBridge, ClassDefinition, ClassEntry, create(), InstanceHandle, AutoCloseable, MemorySegment, T (+1 more)

### Community 54 - "ClassExtractor.kt"
Cohesion: 0.50
Nodes (3): JavaNullCheckConventionsPlugin, Plugin, Project

### Community 56 - "impl/Shared.kt"
Cohesion: 0.19
Nodes (8): ResolvedBuiltinLayout, buildLayoutConstants(), PropertySpec, TypeName, metaToStorageInfo(), primitiveKotlinToCVar(), propertyToStorageConv(), storageToPropertyConv()

### Community 57 - "KNativeImplGen.kt"
Cohesion: 0.42
Nodes (8): BitfieldKind, Builtin, FieldKind, GodotEnumKind, NativeStruct, OpaquePointer, Primitive, Unimplemented

### Community 58 - "RuntimeTypeResolver"
Cohesion: 0.70
Nodes (4): T, load(), loadOrNull(), ResourceLoader

### Community 59 - "StructWrapper.kt"
Cohesion: 0.37
Nodes (12): GDExtensionCallableCustomInfo2(), GDExtensionCallError(), GDExtensionClassCreationInfo5(), GDExtensionClassMethodInfo(), GDExtensionClassVirtualMethodInfo(), GDExtensionInstanceBindingCallbacks(), GDExtensionMainLoopCallbacks(), GDExtensionMethodInfo() (+4 more)

### Community 63 - "Context"
Cohesion: 0.06
Nodes (20): Context, ClassCodegenLevel, GlobalConstant, MethodReturn, TypeMetaHolder, GenericBuiltinInterceptor, GenericConfig, getGenericConfig() (+12 more)

### Community 65 - "BindingInitializationCallbacks"
Cohesion: 0.07
Nodes (18): GDExtensionInitializationLevel, GDExtensionVariantOperator, GDExtensionVariantType, toGDE(), BindingInitializationCallbacks, COpaquePointer, CustomSignalClass, registerCustomSignalClass() (+10 more)

### Community 66 - "FFMUtils"
Cohesion: 0.11
Nodes (15): AddressLayout, FieldLayout, KNativeStructureGenerator, StructLayout, FunctionDescriptor, FFMUtils, OfInt, MemoryLayout (+7 more)

### Community 67 - "ExtractJextractTask"
Cohesion: 0.09
Nodes (23): verifySha256(), ExtractJextractTask, DefaultTask, DirectoryProperty, Property, RegularFileProperty, ByteArray, UByte (+15 more)

### Community 71 - "_copyright"
Cohesion: 0.07
Nodes (27): _copyright, format_version, interface, $schema, /**************************************************************************/, /* a copy of this software and associated documentation files (the        */, /* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,   */, /* Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.                  */ (+19 more)

### Community 72 - "_copyright"
Cohesion: 0.07
Nodes (27): _copyright, format_version, interface, $schema, /**************************************************************************/, /* a copy of this software and associated documentation files (the        */, /* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,   */, /* Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.                  */ (+19 more)

### Community 73 - "Types.kt"
Cohesion: 0.33
Nodes (8): Deprecated, AliasType, EnumType, EnumValue, FunctionType, HandleType, StructType, Types

### Community 74 - "StringConverter.kt"
Cohesion: 0.39
Nodes (8): NodePath, toGodotString(), toNodePath(), toStringName(), toVariant(), toVariantNodePath(), toVariantString(), toVariantStringName()

### Community 75 - "_copyright"
Cohesion: 0.07
Nodes (27): _copyright, format_version, interface, $schema, /**************************************************************************/, /* a copy of this software and associated documentation files (the        */, /* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,   */, /* Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.                  */ (+19 more)

### Community 76 - "_copyright"
Cohesion: 0.07
Nodes (27): _copyright, format_version, interface, $schema, /**************************************************************************/, /* a copy of this software and associated documentation files (the        */, /* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,   */, /* Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.                  */ (+19 more)

### Community 77 - "Auto-registro de clases (Java/Kotlin) — diseño"
Cohesion: 0.25
Nodes (7): Auto-registro de clases (Java/Kotlin) — diseño, Compatibilidad Kotlin, Enfoque propuesto, Estado, Objetivo, Reglas para no romper Graal/Minify, Riesgos

### Community 79 - "model/ClassInfo.kt"
Cohesion: 0.13
Nodes (7): getExportedMethods(), getExportedProperties(), getRpcFunctions(), getToolAnnotation(), FunctionInfo, ParameterInfo, GodotBindingGeneratorScriptRegistryTest

### Community 80 - "KogotProcessor"
Cohesion: 0.14
Nodes (14): CodeGenerator, KSClassDeclaration, KSFile, KSPLogger, DiagnosticLocation, isValidExportType(), KogotProcessor, Provider (+6 more)

### Community 81 - "AbstractCodegenRunner"
Cohesion: 0.19
Nodes (6): CodegenConfig, AbstractCodegenRunner, FileSpec, KotlinNativeRuntimeRunner, FileSpec, RuntimeFFIGenerator

### Community 82 - "BridgeContext"
Cohesion: 0.15
Nodes (5): BridgeContext, Arena, MemorySegment, Override, onSceneDeinit()

### Community 83 - "KotlinCallable"
Cohesion: 0.13
Nodes (5): arguments, KotlinCallable, GDExtensionConstVariantPtr, CallableFactory, GDExtensionConstVariantPtr

### Community 84 - "DiagnosticMessage"
Cohesion: 0.14
Nodes (10): Category, DiagnosticCode, DiagnosticMessage, error(), Severity, ERROR, INFO, WARNING (+2 more)

### Community 88 - "InheritanceTree"
Cohesion: 0.20
Nodes (4): InheritanceTree, register(), renameQualified(), resolveCoreSubpackage()

### Community 89 - "GodotBindingGenerator"
Cohesion: 0.21
Nodes (10): GeneratedFile, GenerationResult, GodotBindingGenerator, godotVirtualTrampolineName(), ClassName, CodeBlock, FunSpec, PropertySpec (+2 more)

### Community 90 - "CodegenOptions"
Cohesion: 0.21
Nodes (6): KotlinNativeBackend, Backend, KotlinNativeApiRunner, FileSpec, GDExtensionInterface, CodegenOptions

### Community 91 - "BindingProcAddressHolder.kt"
Cohesion: 0.36
Nodes (6): BindingProcAddressHolder, initialize(), instance(), instanceOrNull(), GDExtensionClassLibraryPtr, GDExtensionInterfaceGetProcAddress

### Community 93 - "JextractExtension"
Cohesion: 0.06
Nodes (29): IncludesSpec, ListProperty, JextractExtension, JextractVersion, VERSION_19, VERSION_20, VERSION_21, VERSION_22 (+21 more)

### Community 94 - "Agent Directives: Mechanical Overrides"
Cohesion: 0.14
Nodes (13): Agent context rules, Agent Directives: Mechanical Overrides, Agent Orchestration, Code Quality, Context Management, Edit Safety, graphify, How to invoke (+5 more)

### Community 96 - "Godot JVM (godot-java) — Resumen del estado actual"
Cohesion: 0.33
Nodes (5): 1) Qué es y cómo está organizado, 2) Flujo de ejecución (runtime), 3) Estado actual según tus logs, 4) Roadmap sugerido (próximos pasos), Godot JVM (godot-java) — Resumen del estado actual

### Community 98 - "KPoetUtils.kt"
Cohesion: 0.29
Nodes (6): buildLazyBlock(), commonConfiguration(), createFile(), CodeBlock, FileSpec, TypeSpec

### Community 99 - "GodotEnum.kt"
Cohesion: 0.08
Nodes (7): StringName, KotlinScript, GodotError, Script, ScriptLanguage, VariantDictionary, ScriptExtension

### Community 100 - "classCreationInfo6"
Cohesion: 0.06
Nodes (35): CanvasItem, Node, Node2D, RefCounted, Resource, Sprite2D, Boss, Damageable (+27 more)

### Community 101 - "StringNameCache"
Cohesion: 0.46
Nodes (4): Arena, MemorySegment, OfInt, StringNameCache

### Community 102 - "JextractTask"
Cohesion: 0.17
Nodes (10): JextractArgs, JextractTask, ConfigurableFileCollection, DirectoryProperty, Exec, ListProperty, MapProperty, Property (+2 more)

### Community 109 - "BuiltinClassMemberOffsets.kt"
Cohesion: 0.83
Nodes (3): BuiltinClassMemberOffsets, Classes, Members

### Community 110 - "v4_6_0/gdextension_interface.schema.json"
Cohesion: 0.17
Nodes (11): additionalProperties, $id, _copyright, format_version, interface, $schema, types, required (+3 more)

### Community 111 - "AccessorKind"
Cohesion: 0.22
Nodes (8): AccessorKind, DELEGATED, INDEXED, LOCAL, LOCAL_DELEGATED, MISSING, ResolvedAccessor, ResolvedProperty

### Community 113 - "CodegenRunner"
Cohesion: 0.12
Nodes (11): GeneratorBackend, JAVA_FFM, KOTLIN_NATIVE, GeneratorKind, API, CALLABLE, RUNTIME, SIGNAL (+3 more)

### Community 114 - "CallableFactory"
Cohesion: 0.14
Nodes (32): Aabb, Basis, Builtin, Color, Dictionary, AutoCloseable, GodotNative, PackedByteArray (+24 more)

### Community 115 - "castToInternal"
Cohesion: 0.60
Nodes (5): Convert, castTo(), castToInternal(), castToOrNull(), GDExtensionObjectPtr

### Community 116 - "properties"
Cohesion: 0.29
Nodes (7): type, properties, type, type, arguments, legacy_type_name, name

### Community 117 - "ValidationResultImpl"
Cohesion: 0.07
Nodes (22): AbstractFirPhasedDiagnosticTest, AdditionalSourceProvider, KogotFirCheckersExtension, KogotFirExtensionRegistrar, KogotCompilerPluginRegistrar, CompilerConfiguration, AbstractKogotDiagnosticTest, GodotApiSourceProvider (+14 more)

### Community 118 - "DokkaConventionsPlugin"
Cohesion: 0.40
Nodes (3): DokkaConventionsPlugin, Plugin, Project

### Community 119 - "GenerateGodotTask"
Cohesion: 0.06
Nodes (33): GodotCodegenChoreExtension, DirectoryProperty, Property, RegularFileProperty, CodegenBackend, JAVA_FFM, KOTLIN_NATIVE, CodegenKind (+25 more)

### Community 121 - "v4_6_1/gdextension_interface.schema.json"
Cohesion: 0.17
Nodes (11): additionalProperties, $id, _copyright, format_version, interface, $schema, types, required (+3 more)

### Community 126 - "properties"
Cohesion: 0.20
Nodes (12): items, type, items, type, additionalProperties, properties, type, type (+4 more)

### Community 128 - "ImplementationPackageRegistry"
Cohesion: 0.12
Nodes (8): initialize(), initialize(), initialize(), ImplementationPackageRegistry, initialize(), initialize(), UtilityFunctionImplGen, initialize()

### Community 129 - "v4_6_2/gdextension_interface.schema.json"
Cohesion: 0.17
Nodes (11): additionalProperties, $id, _copyright, format_version, interface, $schema, types, required (+3 more)

### Community 130 - "properties"
Cohesion: 0.20
Nodes (12): items, type, items, type, additionalProperties, properties, type, type (+4 more)

### Community 133 - "v4_6_3/gdextension_interface.schema.json"
Cohesion: 0.17
Nodes (11): additionalProperties, $id, _copyright, format_version, interface, $schema, types, required (+3 more)

### Community 134 - "properties"
Cohesion: 0.20
Nodes (12): items, type, items, type, additionalProperties, properties, type, type (+4 more)

### Community 135 - "v4_7_0/gdextension_interface.schema.json"
Cohesion: 0.17
Nodes (11): additionalProperties, $id, _copyright, format_version, interface, $schema, types, required (+3 more)

### Community 136 - "properties"
Cohesion: 0.20
Nodes (12): items, type, items, type, additionalProperties, properties, type, type (+4 more)

### Community 137 - "filterValuesNotNull"
Cohesion: 0.33
Nodes (5): filterValuesNotNull(), T, withExceptionContext(), K, V

### Community 138 - "SyncMode"
Cohesion: 0.50
Nodes (4): Rpc, SyncMode, CALL_LOCAL, CALL_REMOTE

### Community 142 - "v4_7_1/gdextension_interface.schema.json"
Cohesion: 0.17
Nodes (11): additionalProperties, $id, _copyright, format_version, interface, $schema, types, required (+3 more)

### Community 144 - "properties"
Cohesion: 0.20
Nodes (12): items, type, items, type, additionalProperties, properties, type, type (+4 more)

### Community 146 - "GDExtensionCallErrorType"
Cohesion: 0.15
Nodes (10): GDExtensionCallErrorType, withTransientGodotStrings(), withTransientStringNames(), write(), registerProperty(), registerCustomSignal(), PropertyHint, PropertyUsageFlags (+2 more)

### Community 148 - "RecordingHooks"
Cohesion: 0.12
Nodes (13): GodotObject, GodotNative, AutoCloseable, SignalConnection, AutoCloseable, F, GodotError, KProperty (+5 more)

### Community 149 - "JextractRunTask"
Cohesion: 0.33
Nodes (4): JextractRunTask, Exec, ListProperty, RegularFileProperty

### Community 150 - "DownloadJextractTask"
Cohesion: 0.33
Nodes (4): DownloadJextractTask, Property, RegularFileProperty, Download

### Community 151 - "JextractDumpIncludesTask"
Cohesion: 0.29
Nodes (5): JextractDumpIncludesTask, ConfigurableFileCollection, Exec, MapProperty, RegularFileProperty

### Community 152 - "JextractVersionTask"
Cohesion: 0.40
Nodes (3): JextractVersionTask, Exec, RegularFileProperty

### Community 153 - "deprecated"
Cohesion: 0.18
Nodes (11): additionalProperties, properties, required, type, since, deprecated, message, replace_with (+3 more)

### Community 154 - "KotlinLibraryConventionsPlugin"
Cohesion: 0.50
Nodes (3): KotlinLibraryConventionsPlugin, Plugin, Project

### Community 155 - "KotlinStylesConventionsPlugin"
Cohesion: 0.22
Nodes (8): KotlinStylesConventionsPlugin, Plugin, Project, from(), getValue(), getValueOrNull(), T, kotlin

### Community 156 - "KotlinApplicationConventionsPlugin"
Cohesion: 0.50
Nodes (3): KotlinApplicationConventionsPlugin, Plugin, Project

### Community 157 - "JvmToolchainConventionsPlugin"
Cohesion: 0.50
Nodes (3): JvmToolchainConventionsPlugin, Plugin, Project

### Community 158 - "TestingJvmConventionsPlugin"
Cohesion: 0.50
Nodes (3): Plugin, Project, TestingJvmConventionsPlugin

### Community 159 - "JavaStylesConventionsPlugin"
Cohesion: 0.50
Nodes (3): JavaStylesConventionsPlugin, Plugin, Project

### Community 160 - "CommonStylesConventionsPlugin"
Cohesion: 0.50
Nodes (3): CommonStylesConventionsPlugin, Plugin, Project

### Community 161 - "GodotCodegenChorePlugin"
Cohesion: 0.50
Nodes (3): GodotCodegenChorePlugin, Plugin, Project

### Community 163 - "Sprite"
Cohesion: 0.29
Nodes (4): Callable, Vector2, typedCallableSamples(), Sprite

### Community 164 - "deprecated"
Cohesion: 0.18
Nodes (11): additionalProperties, properties, required, type, since, deprecated, message, replace_with (+3 more)

### Community 165 - "deprecated"
Cohesion: 0.18
Nodes (11): additionalProperties, properties, required, type, since, deprecated, message, replace_with (+3 more)

### Community 167 - ".execute"
Cohesion: 0.29
Nodes (4): JavaFfmBackend, Backend, JavaFfmApiRunner, FileSpec

### Community 168 - "deprecated"
Cohesion: 0.18
Nodes (11): additionalProperties, properties, required, type, since, deprecated, message, replace_with (+3 more)

### Community 170 - "deprecated"
Cohesion: 0.18
Nodes (11): additionalProperties, properties, required, type, since, deprecated, message, replace_with (+3 more)

### Community 171 - "deprecated"
Cohesion: 0.18
Nodes (11): additionalProperties, properties, required, type, since, deprecated, message, replace_with (+3 more)

### Community 172 - "BridgeLifecycleTest.java"
Cohesion: 0.07
Nodes (27): _copyright, format_version, interface, $schema, /**************************************************************************/, /* a copy of this software and associated documentation files (the        */, /* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,   */, /* Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.                  */ (+19 more)

### Community 173 - ".shutdownResetsLifecycleState"
Cohesion: 0.12
Nodes (9): KogotCompilerExtension, KogotCompilerGradlePlugin, Project, Property, KogotCompilerGradlePluginTest, KotlinCompilation, KotlinCompilerPluginSupportPlugin, SubpluginArtifact (+1 more)

### Community 174 - "return_value"
Cohesion: 0.20
Nodes (10): items, type, type, description, return_value, type, additionalProperties, properties (+2 more)

### Community 175 - "return_value"
Cohesion: 0.20
Nodes (10): items, type, type, description, return_value, type, additionalProperties, properties (+2 more)

### Community 176 - "return_value"
Cohesion: 0.20
Nodes (10): items, type, type, description, return_value, type, additionalProperties, properties (+2 more)

### Community 178 - "return_value"
Cohesion: 0.20
Nodes (10): items, type, type, description, return_value, type, additionalProperties, properties (+2 more)

### Community 180 - "return_value"
Cohesion: 0.20
Nodes (10): items, type, type, description, return_value, type, additionalProperties, properties (+2 more)

### Community 181 - "return_value"
Cohesion: 0.20
Nodes (10): items, type, type, description, return_value, type, additionalProperties, properties (+2 more)

### Community 182 - "toClassInfo"
Cohesion: 0.17
Nodes (17): KSType, expandTypeAliases(), findVirtualMethodOverride(), getLineNumber(), isGodotObject(), isGodotVirtualMethod(), registeredGodotClassName(), toAnnotationInfo() (+9 more)

### Community 186 - "properties"
Cohesion: 0.22
Nodes (9): const, type, properties, format_version, $schema, types, const, type (+1 more)

### Community 187 - "properties"
Cohesion: 0.22
Nodes (9): const, type, properties, format_version, $schema, types, const, type (+1 more)

### Community 192 - "properties"
Cohesion: 0.22
Nodes (9): const, type, properties, format_version, $schema, types, const, type (+1 more)

### Community 196 - "properties"
Cohesion: 0.22
Nodes (9): const, type, properties, format_version, $schema, types, const, type (+1 more)

### Community 197 - "properties"
Cohesion: 0.22
Nodes (9): const, type, properties, format_version, $schema, types, const, type (+1 more)

### Community 198 - "properties"
Cohesion: 0.22
Nodes (9): const, type, properties, format_version, $schema, types, const, type (+1 more)

### Community 199 - "registerMethod"
Cohesion: 0.57
Nodes (6): GDExtensionClassMethodCall, MethodArgument, registerMethod(), registerMethodGetter(), registerMethodSetter(), MethodFlags

### Community 200 - "required"
Cohesion: 0.22
Nodes (10): items, items, type, additionalProperties, required, arguments, description, kind (+2 more)

### Community 201 - "generate"
Cohesion: 0.36
Nodes (3): KotlinNativeCallableGenerator, FileSpec, FunSpec

### Community 202 - "enum"
Cohesion: 0.25
Nodes (8): alias, enum, function, handle, struct, enum, type, kind

### Community 203 - "required"
Cohesion: 0.25
Nodes (8): allOf, required, unevaluatedProperties, arguments, description, kind, name, items

### Community 204 - "enum"
Cohesion: 0.25
Nodes (8): alias, enum, function, handle, struct, enum, type, kind

### Community 206 - "required"
Cohesion: 0.25
Nodes (8): allOf, required, unevaluatedProperties, arguments, description, kind, name, items

### Community 208 - "enum"
Cohesion: 0.25
Nodes (8): alias, enum, function, handle, struct, enum, type, kind

### Community 211 - "required"
Cohesion: 0.25
Nodes (8): allOf, required, unevaluatedProperties, arguments, description, kind, name, items

### Community 212 - "enum"
Cohesion: 0.25
Nodes (8): alias, enum, function, handle, struct, enum, type, kind

### Community 213 - "CallableTrampolines"
Cohesion: 0.22
Nodes (8): GDExtensionCallableCustomEqual, GDExtensionCallableCustomFree, GDExtensionCallableCustomGetArgumentCount, GDExtensionCallableCustomHash, GDExtensionCallableCustomIsValid, GDExtensionCallableCustomLessThan, GDExtensionCallableCustomToString, CallableTrampolines

### Community 216 - "required"
Cohesion: 0.25
Nodes (8): allOf, required, unevaluatedProperties, arguments, description, kind, name, items

### Community 223 - "enum"
Cohesion: 0.25
Nodes (8): alias, enum, function, handle, struct, enum, type, kind

### Community 224 - "checkGodotError"
Cohesion: 0.33
Nodes (3): PropertyInfo, SignalParameterInfo, SignalRegistrationTest

### Community 229 - "registerCustomSignal"
Cohesion: 0.23
Nodes (14): classCompatibility(), combineAll(), containerCompatibility(), isGodotObject(), KogotClassIds, ClassId, typeParameterCompatibility(), VariantCompatibility (+6 more)

### Community 230 - "NodeVirtualCalls"
Cohesion: 0.08
Nodes (14): GDExtensionClassCallVirtual, GDExtensionStringNamePtr, checkGodotError(), GodotError, createInstanceFunc(), GDExtensionObjectPtr, resolveVirtualCall(), KotlinResourceFormatLoader (+6 more)

### Community 231 - "NodeVirtualDispatcher"
Cohesion: 0.24
Nodes (9): fixed2d3d(), isAllUpperCase(), isKotlinKeyword(), renameAllUpperCaseToCamelCase(), renameGodotClass(), safeIdentifier(), sanitizeTypeName(), screamingToPascalCase() (+1 more)

### Community 234 - "required"
Cohesion: 0.25
Nodes (8): allOf, required, unevaluatedProperties, arguments, description, kind, name, items

### Community 235 - "enum"
Cohesion: 0.25
Nodes (8): alias, enum, function, handle, struct, enum, type, kind

### Community 236 - "DefaultVariantTypeResolver"
Cohesion: 0.32
Nodes (3): DefaultVariantTypeResolver, ClassName, VariantTypeResolver

### Community 238 - "type"
Cohesion: 0.20
Nodes (10): items, type, allOf, type, unevaluatedProperties, _copyright, see, items (+2 more)

### Community 239 - "type"
Cohesion: 0.29
Nodes (7): items, type, type, _copyright, see, items, type

### Community 240 - "asList"
Cohesion: 0.11
Nodes (11): AnnotationInfo, getArgument(), T, getExportMethodAnnotation(), getRpcAnnotation(), Builtin, getExportAnnotation(), getRegisterSignalAnnotation() (+3 more)

### Community 241 - "exported/build.gradle.kts"
Cohesion: 0.06
Nodes (29): Family, KogotExtension, DirectoryProperty, ListProperty, MapProperty, Property, RegularFileProperty, KogotPlugin (+21 more)

### Community 242 - "godotKotlinInit"
Cohesion: 0.27
Nodes (8): GodotExecTask, DirectoryProperty, Exec, ListProperty, Property, RegularFileProperty, RunGodotTask, TestGodotTask

### Community 245 - "type"
Cohesion: 0.29
Nodes (7): items, type, type, _copyright, see, items, type

### Community 246 - "type"
Cohesion: 0.29
Nodes (7): items, type, type, _copyright, see, items, type

### Community 247 - "type"
Cohesion: 0.29
Nodes (7): items, type, type, _copyright, see, items, type

### Community 248 - "type"
Cohesion: 0.29
Nodes (7): items, type, type, _copyright, see, items, type

### Community 251 - "track.sh"
Cohesion: 0.60
Nodes (4): error(), info(), resolve_workdir(), track.sh script

### Community 257 - "model/AnnotationInfo.kt"
Cohesion: 0.33
Nodes (3): KotlinNativeSignalGenerator, FileSpec, FunSpec

### Community 319 - "source/build.gradle.kts"
Cohesion: 0.25
Nodes (6): GenerateEntryPointTask, DefaultTask, DirectoryProperty, FileSpec, Property, MemberName

### Community 454 - "BuildConfiguration"
Cohesion: 0.24
Nodes (9): checkAndNormalizeTypeName(), BuildConfiguration, DOUBLE_32, DOUBLE_64, FLOAT_32, FLOAT_64, defaultFor(), fromJsonName() (+1 more)

### Community 455 - "godotKotlinInit"
Cohesion: 0.12
Nodes (6): GenerateGdextensionTask, DefaultTask, MapProperty, Property, RegularFileProperty, GenerateGdextensionTaskTest

### Community 456 - ".buildTestContext"
Cohesion: 0.17
Nodes (11): additionalProperties, $id, _copyright, format_version, interface, $schema, types, required (+3 more)

### Community 457 - "GDExtensionVariantType"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 458 - "registerSignalsForCustomSignalClass"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 459 - "Generic virtual method dispatch (issue #20)"
Cohesion: 0.25
Nodes (7): Design, Engine-class/singleton arguments are always nullable (see issue #141), Engine-class/singleton returns are always nullable (see issue #143), Generic virtual method dispatch (issue #20), Problem, Type-support matrix, Where things live

### Community 460 - "getInstanceBinding"
Cohesion: 0.20
Nodes (12): items, type, items, type, additionalProperties, properties, type, type (+4 more)

### Community 462 - "Provider"
Cohesion: 0.18
Nodes (11): additionalProperties, properties, required, type, since, deprecated, message, replace_with (+3 more)

### Community 463 - "KotlinMultiplatformConventionsPlugin"
Cohesion: 0.16
Nodes (7): commonConfiguration(), KotlinJvmCommonConventionsPlugin, Plugin, Project, KotlinMultiplatformConventionsPlugin, Plugin, Project

### Community 472 - "KogotDiagnostics"
Cohesion: 0.22
Nodes (8): BaseDiagnosticRendererFactory, KogotDiagnosticRenderers, KogotDiagnostics, ConeKotlinType, KtDiagnosticFactory1, KtDiagnosticFactory2, KtDiagnosticFactory3, KtDiagnosticsContainer

### Community 473 - ".createCustomInfo2"
Cohesion: 0.29
Nodes (6): GDExtensionCallableCustomCall, CallableBinding, COpaquePointer, CValue, GodotNative, ULong

### Community 474 - "return_value"
Cohesion: 0.20
Nodes (10): items, type, type, description, return_value, type, additionalProperties, properties (+2 more)

### Community 477 - "createKotlinScriptInstance"
Cohesion: 0.31
Nodes (7): COpaquePointer, createKotlinScriptInstance(), internedName(), COpaquePointer, CPointer, ScriptInstanceState, state()

### Community 478 - "openTypes.kt"
Cohesion: 0.31
Nodes (7): accepted(), GodotNative, UnrelatedAbstract, UnrelatedOpen, UserFunInterface, UserInterface, UserSealedInterface

### Community 479 - "Virtual dispatch: engine-class argument nullability (issue #141)"
Cohesion: 0.22
Nodes (8): Impact, Outcome, Prior art in this lineage, Problem, Scope, Verification (mirroring #135/#137/#139's methodology — real exit codes, no exit-code-masking pipes), Virtual dispatch: engine-class argument nullability (issue #141), What is NOT verified

### Community 480 - "Virtual dispatch: engine-class return nullability (issue #143)"
Cohesion: 0.22
Nodes (8): Impact, Outcome, Prior art in this lineage, Problem, Scope, Verification (mirroring #135/#137/#139/#141's methodology — real exit codes, no exit-code-masking pipes), Virtual dispatch: engine-class return nullability (issue #143), What is NOT verified

### Community 481 - "properties"
Cohesion: 0.22
Nodes (9): const, type, properties, format_version, $schema, types, const, type (+1 more)

### Community 483 - "complexExpressions.kt"
Cohesion: 0.25
Nodes (3): Box, identity(), T

### Community 484 - "Virtual dispatch: void* return support (issue #137)"
Cohesion: 0.25
Nodes (7): Outcome, Prior art in the codebase, Problem, Scope, Verification (mirroring #135's corrected methodology — real exit codes, no exit-code-masking pipes), Virtual dispatch: void* return support (issue #137), What is NOT verified

### Community 485 - "Virtual dispatch: typedarray::* return support (issue #139)"
Cohesion: 0.25
Nodes (7): Outcome, Prior art in the codebase, Problem, Scope, Verification (mirroring #135/#137's methodology — real exit codes, no exit-code-masking pipes), Virtual dispatch: typedarray::* return support (issue #139), What is NOT verified

### Community 486 - "required"
Cohesion: 0.25
Nodes (8): allOf, required, unevaluatedProperties, arguments, description, kind, name, items

### Community 487 - "enum"
Cohesion: 0.25
Nodes (8): alias, enum, function, handle, struct, enum, type, kind

### Community 489 - ".execute"
Cohesion: 0.29
Nodes (3): KotlinNativeSignalRunner, FileSpec, NativeSignalPackageRegistry

### Community 491 - "typeAliases"
Cohesion: 0.29
Nodes (6): AABB, RID, typeAliases(), Pattern, Position, Positions

### Community 492 - "Virtual dispatch: heap-backed builtin return support (issue #135)"
Cohesion: 0.29
Nodes (6): A real bug my first verification pass missed, Outcome, Prior art in the codebase, Problem, Scope, Virtual dispatch: heap-backed builtin return support (issue #135)

### Community 493 - "Kotlin as a Godot Script Language (issue #42) — design notes"
Cohesion: 0.29
Nodes (6): Core constraint, Explicitly out of scope for #42, File → class mapping, Implementation phases, Kotlin as a Godot Script Language (issue #42) — design notes, Problem

### Community 494 - "type"
Cohesion: 0.29
Nodes (7): items, type, type, _copyright, see, items, type

### Community 496 - "kogot Gradle Plugin"
Cohesion: 0.29
Nodes (6): Building / testing this composite build, Configuration (`kogot { ... }` extension), Generated tasks, kogot Gradle Plugin, `plugin`: `io.github.kingg22.kogot`, `settings-plugin`: `io.github.kingg22.kogot.settings`

### Community 499 - "signal"
Cohesion: 0.50
Nodes (4): F, ReadOnlyProperty, signal(), TypedSignal

### Community 500 - "collections.kt"
Cohesion: 0.40
Nodes (3): addTwice(), Box, T

### Community 505 - "inspect-object-repro"
Cohesion: 0.50
Nodes (3): inspect-object-repro, Usage, Why this exists

## Knowledge Gaps
- **956 isolated node(s):** `KOTLIN_NATIVE`, `JAVA_FFM`, `API`, `RUNTIME`, `CALLABLE` (+951 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **255 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `KotlinNativeCallableGenerator` connect `generate` to `models/ClassInfo.kt`?**
  _High betweenness centrality (0.042) - this node is a cross-community bridge._
- **Why does `GodotObject` connect `RecordingHooks` to `classCreationInfo6`, `ApiFilters`, `CallableFactory`, `castToInternal`, `signal`, `createKotlinScriptInstance`?**
  _High betweenness centrality (0.030) - this node is a cross-community bridge._
- **Why does `CodegenConfig` connect `AbstractCodegenRunner` to `models/ClassInfo.kt`, `.execute`, `.execute`, `ApiItem`, `CodegenRunner`, `GenerateGodotTask`, `CodegenOptions`?**
  _High betweenness centrality (0.027) - this node is a cross-community bridge._
- **What connects `KOTLIN_NATIVE`, `JAVA_FFM`, `API` to the rest of the system?**
  _956 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `GodotVersion` be split into smaller, more focused modules?**
  _Cohesion score 0.05137844611528822 - nodes in this community are weakly interconnected._
- **Should `TypeOverloadGenerator.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.08700564971751412 - nodes in this community are weakly interconnected._
- **Should `EnumMask` be split into smaller, more focused modules?**
  _Cohesion score 0.060451977401129946 - nodes in this community are weakly interconnected._