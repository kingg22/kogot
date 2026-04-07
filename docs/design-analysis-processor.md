# Arquitectura KSP Processor para Kogot Bindings

## Context

Necesitamos un KSP processor robusto que:
1. Extraiga información de anotaciones de los devs (`@Export`, `@Rpc`, `@Tool`, etc.)
2. Valide el contrato de esas anotaciones
3. Genere el código de registro para el binding de Godot (Kotlin y/o JSON según flag)
4. Sea extensible para nuevas reglas y generadores
5. Tenga un módulo `analysis` propio y reutilizable en IDE plugin
6. Tenga errores de nivel rustc

---

## Arquitectura de Módulos Gradle

```
kogot/
├── analysis/                           # Módulo reusable (KSP-agnostic)
│   └── src/main/kotlin/io/github/kingg22/kogot/analysis/
│       ├── models/                     # ClassInfo, FunctionInfo, etc. (sin KS)
│       ├── extractors/                 # ClassExtractor, FunctionExtractor
│       ├── resolvers/                  # TypeResolver, AnnotationResolver
│       └── context/                    # AnalysisContext interface
│
├── processor/                          # KSP SymbolProcessor
│   └── src/main/kotlin/io/github/kingg22/kogot/processor/
│       ├── KogotProcessor.kt           # Entry point
│       ├── KogotOptions.kt             # Config (incl. output mode: KT | JSON | BOTH)
│       ├── diagnostics/               # Rustc-level errors
│       ├── validation/                # Validators
│       ├── transformation/            # Enrichers
│       ├── generation/                # Generators (kt + json)
│       │   ├── kotlin/                 # KotlinGenerator, BindingGenerator
│       │   └── json/                   # JsonGenerator
│       └── bridge/                    # KspAnalysisContext (KSP → Analysis)
│
├── processor-test/                     # Tests
└── codegen/                            # (ya existe)
```

**Punto clave**: El módulo `analysis` es 100% agnóstico de KSP. Puede reutilizarse en un IDE plugin.

---

## Arquitectura General: Capas + Pipeline de Fases

```
┌─────────────────────────────────────────────────────────────────┐
│                     KSP SymbolProcessor                         │
│                  (io.github.kingg22.kogot.processor)            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              INFRAESTRUCTURE LAYER                         │ │
│  │  • KSPContext (KSResolver, KSCodeGenerator, Env)           │ │
│  │  • DiagnosticReporter (rustc-level)                        │ │
│  │  • PhaseTimer (timeline por fase)                          │ │
│  │  • KogotOptions (outputMode: KT | JSON | BOTH)             │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              ↓                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              ANALYSIS LAYER (KSP-Agnostic, módulo shared)  │ │
│  │  • Models: ClassInfo, FunctionInfo, PropertyInfo           │ │
│  │  • Extractors: ClassExtractor, FunctionExtractor, etc.     │ │
│  │  • Resolvers: TypeResolver, AnnotationResolver             │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              ↓                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              VALIDATION LAYER                              │ │
│  │  • ValidationContext (shared state)                        │ │
│  │  • Validators: ExportValidator, RpcValidator, etc.         │ │
│  │  • ValidationResult (errors/warnings aggregation)          │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              ↓                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              TRANSFORMATION LAYER                          │ │
│  │  • Enrichers: ClassEnricher, FunctionEnricher              │ │
│  │  • ResolvedModels: EnrichedClass, EnrichedFunction         │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              ↓                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              GENERATION LAYER (multi-output)               │ │
│  │  • Generator interface                                     │ │
│  │  • KotlinGenerator → .kt binding code                      │ │
│  │  • JsonGenerator → manifest.json                           │ │
│  │  • OutputMode decide cual/cuales se ejecutan               │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Detalle de Capas

### 1. Infrastructure Layer

```
processor/src/main/kotlin/io/github/kingg22/kogot/processor/
├── KogotProcessor.kt              # SymbolProcessor entry point
├── KogotOptions.kt                 # Configuración (outputMode, etc.)
├── KogotLogger.kt                  # Wrapper sobre KSPLogger
├── diagnostics/
│   ├── DiagnosticContext.kt       # Timeline + errores
│   ├── DiagnosticReporter.kt      # Interface rustc-level
│   ├── DiagnosticCode.kt          # KOGOT001, KOGOT002, etc.
│   ├── DiagnosticLocation.kt      # File, line, column
│   ├── DiagnosticMessage.kt       # Mensaje + help + note
│   ├── DiagnosticRenderer.kt      # Adapta a KSP/IDE/text
│   └── ChronologicalDiagnostics.kt
└── pipeline/
    └── ProcessorPipeline.kt       # Orchestrates fases
```

### 2. Analysis Layer (`analysis/` module, KSP-agnostic)

```
analysis/src/main/kotlin/io/github/kingg22/kogot/analysis/
├── models/
│   ├── ClassInfo.kt              # Modelo limpio de clase
│   ├── FunctionInfo.kt           # Modelo limpio de función
│   ├── PropertyInfo.kt           # Modelo limpio de propiedad
│   ├── ParameterInfo.kt          # Modelo limpio de parámetro
│   ├── TypeInfo.kt               # Tipo resuelto (sin KS)
│   └── AnnotationInfo.kt         # Anotación con parámetros
├── extractors/
│   ├── KotlinSourceParser.kt     # Parsea fuente Kotlin (sin KSP)
│   ├── ClassExtractor.kt         # Interface + implementación
│   ├── FunctionExtractor.kt
│   ├── PropertyExtractor.kt
│   └── AnnotationExtractor.kt
├── resolvers/
│   ├── TypeResolver.kt           # Resuelve tipos (sin KS)
│   └── AnnotationResolver.kt    # Extrae anotaciones
└── context/
    └── AnalysisContext.kt        # Interface abstracta
```

**Principio clave**: Los modelos (`ClassInfo`, `FunctionInfo`, etc.) NO contienen tipos de KSP. Solo strings, enums, y estructuras de datos.

### 3. Validation Layer

```
processor/src/main/kotlin/io/github/kingg22/kogot/processor/validation/
├── ValidationContext.kt          # Estado compartido entre validators
├── ValidationResult.kt           # Interface para acumular errores
├── ValidationResultImpl.kt       # Implementación
├── Validator.kt                  # Interface base
├── ValidatorPipeline.kt          # Ejecuta validators en secuencia
└── validators/
    ├── ExportValidator.kt        # Valida @Export y variantes
    ├── RpcValidator.kt           # Valida @Rpc
    ├── ToolValidator.kt          # Valida @Tool
    ├── NamingValidator.kt        # Valida convenciones de nombres
    ├── TypeCompatibilityValidator.kt
    └── GroupNamingValidator.kt   # Valida @ExportGroup/prefijos
```

### 4. Transformation Layer

```
processor/src/main/kotlin/io/github/kingg22/kogot/processor/transformation/
├── enrichers/
│   ├── ClassEnricher.kt         # Añade info adicional a ClassInfo
│   ├── FunctionEnricher.kt
│   └── PropertyEnricher.kt
├── models/
│   ├── EnrichedClass.kt          # ClassInfo + resolved types + hints
│   ├── EnrichedFunction.kt
│   └── EnrichedProperty.kt
└── ResolvedTypeRegistry.kt      # Cache de tipos resueltos
```

### 5. Generation Layer

```
processor/src/main/kotlin/io/github/kingg22/kogot/processor/generation/
├── Generator.kt                  # Interface principal
├── GeneratorContext.kt           # Contexto para generación
├── kotlin/
│   ├── KotlinGenerator.kt        # Genera .kt binding code
│   ├── ClassRegistrationGenerator.kt
│   ├── PropertyRegistrationGenerator.kt
│   ├── RpcRegistrationGenerator.kt
│   └── SignalRegistrationGenerator.kt
└── json/
    ├── JsonGenerator.kt          # Genera manifest.json
    └── JsonSchema.kt             # Schema del JSON output
```

**Patrón Generator:**
```kotlin
interface Generator {
    val name: String
    fun generate(context: GeneratorContext, models: List<EnrichedClass>): GeneratedOutput
}

data class GeneratedOutput(
    val files: List<GeneratedFile>,
    val diagnostics: List<DiagnosticMessage>
)

enum class OutputMode { KOTLIN, JSON, BOTH }
```

---

## Diagnostic Reporter (Rustc-level DX)

```
processor/src/main/kotlin/io/github/kingg22/kogot/processor/diagnostics/
├── DiagnosticCode.kt             # KOGOT-001, KOGOT-002, etc.
├── DiagnosticLocation.kt         # File, line, column
├── DiagnosticMessage.kt          # main message + help + note
└── DiagnosticRenderer.kt         # Adapta output a KSP logger / IDE / text
```

**Formato de error estilo rustc:**
```
error[KOGOT-001]: @Export on invalid type
  --> src/MyNode.kt:15:5
   |
15 |     @Export var invalidProp: SomeClass
   |         ^^^^^^^ Unsupported type 'SomeClass'. Supported: primitives, Godot types.
   |
   = help: Use a Godot built-in type or register a custom class
   = note: This property will not appear in the Inspector
```

**Componentes:**
- `DiagnosticCode`: Identificador único (KOGOT001, KOGOT002, etc.)
- `DiagnosticLocation`: Precisa (file, line, column)
- `DiagnosticMessage`: Mensaje principal + ayuda secundaria
- `DiagnosticRenderer`: Adapta el output al target (KSP logger, IDE, text)

---

## Fase Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│                    PROCESSOR PIPELINE                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. EXTRACTION PHASE                                            │
│     └─ KSP → Clean Models (via KspAnalysisContext bridge)       │
│     └─ Extrae anotaciones y metadatos                           │
│     └─ Detecta símbolos no resueltos (defer)                    │
│                                                                 │
│  2. VALIDATION PHASE                                            │
│     └─ Ejecuta validators en secuencia                          │
│     └─ Cada validator reporta errores específicos               │
│     └─ Pipeline continua si hay errores no-fatales              │
│     └─ Errores fatales detienen el pipeline                     │
│                                                                 │
│  3. TRANSFORMATION PHASE                                        │
│     └─ Resolver tipos faltantes                                 │
│     └─ Enriquecer modelos con info derivada                     │
│     └─ Generar EnrichedClass, EnrichedFunction, etc.            │
│                                                                 │
│  4. GENERATION PHASE (según OutputMode)                         │
│     └─ KOTLIN: ejecuta KotlinGenerator                          │
│     └─ JSON: ejecuta JsonGenerator                              │
│     └─ BOTH: ejecuta ambos                                      │
│                                                                 │
│  5. REPORTING PHASE                                             │
│     └─ Render diagnostics finales                               │
│     └─ Devolver símbolos no procesados                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Extensibilidad

### Nuevas Anotaciones

1. Crear `AnnotationInfo` en `analysis/models/`
2. Crear extractor en `analysis/extractors/`
3. Crear validator en `validation/validators/`
4. Opcional: crear enricher y generator

### Nuevos Generadores

1. Implementar `Generator` interface
2. Registrar via SPI: `META-INF/services/GeneratorProvider`
3. El processor cargará todos los generators automáticamente

### IDE Plugin Support

El módulo `analysis/` es completamente agnóstico de KSP. Para un IDE plugin:

```
ide-plugin/
├── analysis/                        # (reused from kogot/analysis module)
└── ide/
    ├── IdeaAnalysisContext.kt       # Bridge IDE → Analysis
    ├── InspectionRegistry.kt
    └── HighlightRenderer.kt
```

---

## Testing Strategy

### Sin KSP (Unit Tests)

```kotlin
// Test de ClassExtractor sin KSP
@Test
fun `ClassExtractor extracts class info correctly`() {
    val extractor = ClassExtractor(annotationResolver)
    val source = """..."""
    val classInfo = extractor.extract(parseKotlin(source))

    assertEquals("MyNode", classInfo.name)
    assertEquals(2, classInfo.properties.size)
    assertTrue(classInfo.annotations.hasExport())
}
```

### Con KSP (Integration Tests)

```kotlin
@Test
fun `processor generates binding for @Export annotated class`() {
    val result = compile(
        """
        @Tool
        class MyNode : Node {
            @Export var health: Int = 100
        }
        """
    )

    assertThat(result).generatedFile("MyNodeBinding.kt")
        .contains("registerProperty(\"health\", INT)")
}
```

---

## Verification

1. **Unit tests**: Cada extractor, validator, enricher probados aisladamente (sin KSP)
2. **Integration tests**: KSP processor completo con test fixtures
3. **Golden tests**: Comparar output generado contra archivos esperados
4. **Error rendering tests**: Verificar formato de errores en diferentes targets

---

## Archivos Críticos a Crear (orden de implementación sugerido)

| Fase      | Archivo                                                                 | Propósito                  |
|-----------|-------------------------------------------------------------------------|----------------------------|
| Analysis  | `analysis/src/main/.../analysis/models/ClassInfo.kt`                    | Modelo limpio de clase     |
| Analysis  | `analysis/src/main/.../analysis/models/FunctionInfo.kt`                 | Modelo limpio de función   |
| Analysis  | `analysis/src/main/.../analysis/models/PropertyInfo.kt`                 | Modelo limpio de propiedad |
| Analysis  | `analysis/src/main/.../analysis/extractors/ClassExtractor.kt`           | Interface + impl           |
| Analysis  | `analysis/src/main/.../analysis/context/AnalysisContext.kt`             | Interface abstracta        |
| Processor | `processor/src/main/.../processor/bridge/KspAnalysisContext.kt`         | Bridge KSP → Analysis      |
| Processor | `processor/src/main/.../processor/diagnostics/DiagnosticCode.kt`        | Identificador único        |
| Processor | `processor/src/main/.../processor/diagnostics/DiagnosticMessage.kt`     | Mensaje con help/note      |
| Processor | `processor/src/main/.../processor/diagnostics/DiagnosticRenderer.kt`    | Adapta a target            |
| Processor | `processor/src/main/.../processor/validation/Validator.kt`              | Interface base             |
| Processor | `processor/src/main/.../processor/validation/ValidatorPipeline.kt`      | Orchestra validators       |
| Processor | `processor/src/main/.../processor/generation/Generator.kt`              | Interface principal        |
| Processor | `processor/src/main/.../processor/generation/kotlin/KotlinGenerator.kt` | Genera .kt                 |
| Processor | `processor/src/main/.../processor/generation/json/JsonGenerator.kt`     | Genera .json               |
| Processor | `processor/src/main/.../processor/KogotProcessor.kt`                    | Entry point                |
| Processor | `processor/src/main/.../processor/KogotOptions.kt`                      | Config + OutputMode        |

---

## Referencias de Patrones

- **ktorgen**: DiagnosticTimer timeline, ValidatorPipeline, DeclarationMapper pattern
- **utopia**: Task/Rule hierarchy, FileBuilder pattern, Check/Validator pattern
- **androidx-room**: XProcessing abstraction (KSP-agnostic interfaces), Element stores (caching), sealed type hierarchies, XEquality for proper wrapper equality
- **Clean Architecture**: Separation of concerns, Dependency Inversion, Single Responsibility
- **Rustc**: Formato de errores con código, ubicación, ayuda secundaria

---

## Inspiración Adicional: XProcessing de Room

El módulo `room3-compiler-processing/` de androidx Room provee una **abstracción completa sobre KSP** que podemos replicar.

### Patrón Central: Interfaces XProcessing (Backend-Agnostic)

```
room3-compiler-processing/src/main/java/androidx/room3/compiler/processing/
├── XProcessingEnv.kt          # Environment interface
├── XElement.kt               # Base element interface
├── XType.kt                 # Type interface (sin KSP)
├── XTypeElement.kt          # Class/interface declarations
├── XExecutableElement.kt    # Methods/constructors
├── XFieldElement.kt         # Fields
├── XAnnotated.kt            # Annotation access
├── XRoundEnv.kt             # Round information
├── XBasicAnnotationProcessor.kt  # Processor base
└── XEquality.kt             # Equality for wrappers
```

### KSP Implementation (Bridge)

```
room3-compiler-processing/src/main/java/androidx/room3/compiler/processing/ksp/
├── KspProcessingEnv.kt      # Implementa XProcessingEnv
├── KspElement.kt            # Base wrapper para KSAnnotated
├── KspType.kt               # Base wrapper para KSType
├── KspTypeElement.kt        # Wrapper para KSClassDeclaration
├── KspMethodElement.kt      # Wrapper para KSFunctionDeclaration
├── KspFieldElement.kt       # Wrapper para KSPropertyDeclaration
└── KspMessager.kt          # Bridge para KSPLogger
```

### Patrones Clave de Room

**1. Wrapper con Delegation:**
```kotlin
internal sealed class KspTypeElement(
    env: KspProcessingEnv,
    override val declaration: KSClassDeclaration,
) : KspElement(env, declaration),
    XTypeElement,
    XHasModifiers by KspHasModifiers.create(declaration),
    XAnnotated by KspAnnotated.create(env, declaration, NO_USE_SITE)
```

**2. Factory Methods para Type Dispatch:**
```kotlin
fun wrap(ksType: KSType, allowPrimitives: Boolean): KspType {
    return when {
        ksType.declaration is KSTypeParameter -> KspTypeVariableType(env, ksType)
        allowPrimitives && ksType.isPrimitiveType() -> KspPrimitiveType(env, ksType)
        else -> DefaultKspType(env, ksType)
    }
}
```

**3. Caching via Element Stores:**
```kotlin
private val typeElementStore = XTypeElementStore(
    findElement = { resolver.getClassDeclarationByName(it) },
    wrap = { classDeclaration -> KspTypeElement.create(env, classDeclaration) }
)
```

### Aplicación a Nuestro Diseño

Nuestro módulo `analysis/` seguirá el mismo patrón:

```
analysis/src/main/kotlin/io/github/kingg22/kogot/analysis/
├── context/
│   └── AnalysisContext.kt        # Interface (equivalente a XProcessingEnv)
├── models/
│   ├── ClassInfo.kt             # Modelo puro (equivalente a XTypeElement simplificado)
│   ├── FunctionInfo.kt           # Modelo puro
│   └── ...
└── extractors/
    └── KotlinSourceParser.kt     # Parsea fuente (sin KSP)
```

**En processor/ bridge:**
```
processor/src/main/kotlin/.../processor/bridge/
└── KspAnalysisContext.kt         # Equivalente a KspProcessingEnv de Room
```

### Test Suite de Room

```
room3-compiler-processing-testing/
├── src/main/java/.../util/
│   ├── Source.kt                  # Crea archivos de test (Java/Kotlin)
│   └── ProcessorTestExt.kt       # runKspTest(), runProcessorTest()
└── src/test/java/.../ksp/
    └── KspTypeTest.kt            # Ejemplo de tests KSP
```

**Patrón de test:**
```kotlin
@Test
fun typeArguments() {
    val src = Source.kotlin("foo.kt", """
        class Subject {
            val list: List<String?> = TODO()
        }
    """.trimIndent())
    runProcessorTest(listOf(src)) { invocation ->
        val subject = invocation.processingEnv.requireTypeElement("Subject")
        subject.getField("list").type.let { type ->
            assertThat(type.nullability).isEqualTo(NONNULL)
        }
    }
}
```

---

## Generación de Código con KotlinPoet

KotlinPoet es la librería estándar para generar código Kotlin desde otros programas (equivalente a JavaPoet para Java).

```kotlin
// Ejemplo simple de KotlinPoet
val file = FileSpec.builder("com.example", "MyBinding")
    .addType(TypeSpec.classBuilder("MyBinding")
        .addFunction(FunSpec.builder("register")
            .addStatement("println(%S)", "Hello")
            .build())
        .build())
    .build()

file.writeTo(System.out)
```

**Uso en nuestro diseño:** `KotlinGenerator` usará KotlinPoet para generar el código .kt del binding en lugar de strings concatenados.

### Dependencia Gradle

```kotlin
// processor/build.gradle.kts
dependencies {
    implementation("com.squareup.kotlinpoet:kotlinpoet:1.18.0")
}
```

---

## ClassGraph vs KSP (Para Entender Utopia)

### ¿Qué es ClassGraph?

ClassGraph escanea **bytecode compilado** (.class) en lugar de código fuente. Es útil cuando necesitas procesar múltiples lenguajes JVM (Kotlin, Java, Scala, Groovy) porque todos compilan a bytecode.

### Comparación

| Aspecto                | KSP                        | ClassGraph                       |
|------------------------|----------------------------|----------------------------------|
| Trabaja sobre          | Código fuente (AST)        | Bytecode compilado               |
| Soporte multi-lenguaje | Solo Kotlin (+Java básico) | Cualquier lenguaje JVM           |
| Información de línea   | Exacta                     | Solo del bytecode, no del origen |
| Incremental            | Sí, integrado              | No, escaneo completo             |
| Mantenimiento          | Depende de versión Kotlin  | Estable (bytecode no cambia)     |

### ¿Por qué Utopia usa ClassGraph?

Utopia soporta **múltiples lenguajes JVM**: Kotlin, Java, **Scala**, Groovy. KSP solo funciona bien con Kotlin. ClassGraph lee bytecode, y **todo lenguaje JVM compila a bytecode**. Por eso lo eligieron.

### ¿Nos sirve ClassGraph?

**Probablemente no** para el processor principal, porque:
- Solo procesamos Kotlin
- Necesitamos línea exacta para errores
- KSP tiene incremental processing

**Sí nos serviría** si quisiéramos:
- Un IDE plugin que lea bytecode de dependencias ya compiladas
- Soportar otros lenguajes en el futuro
