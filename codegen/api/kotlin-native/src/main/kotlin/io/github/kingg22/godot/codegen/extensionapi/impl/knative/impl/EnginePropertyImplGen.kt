package io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.BodyGenerator
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.addKdocIfPresent
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.experimentalApiAnnotation
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedEngineClass
import io.github.kingg22.godot.codegen.types.PRIMITIVE_NUMERIC_TYPES
import io.github.kingg22.godot.codegen.utils.withExceptionContext

// ===============================
// Unified Property Generation (Accessor-level Strategy)
// ===============================
class EnginePropertyImplGen(private val typeResolver: TypeResolver, private val body: EngineMethodImplGen) {

    /**
     * 🔥 NUEVO ENFOQUE:
     *
     * En vez de tener una sola estrategia por property (DIRECT / DELEGATED / etc),
     * ahora resolvemos CADA accessor (getter/setter) de forma independiente.
     *
     * Esto permite combinaciones como:
     * - getter LOCAL + setter DELEGATED
     * - getter DELEGATED + setter LOCAL
     * - getter INDEXED + setter FALLBACK
     *
     * Mucho más cercano a cómo realmente funciona el API de Godot.
     */

    private enum class AccessorKind {
        LOCAL, // Método existe en la clase actual
        LOCAL_DELEGATED, // tiene más de un argumento
        DELEGATED, // Método existe en clase padre
        INDEXED, // Usa índice (Side, Axis, etc)
        MISSING, // No existe → fallback
    }

    private data class ResolvedAccessor(val method: EngineClass.ClassMethod?, val kind: AccessorKind) {
        val isCallableAsProperty = when (kind) {
            LOCAL, DELEGATED -> method!!.arguments.size == 1
            INDEXED -> method!!.arguments.size >= 2
            LOCAL_DELEGATED -> method!!.arguments.count { it.defaultValue == null } == 1
            MISSING -> false
        }
    }

    private data class ResolvedProperty(val getter: ResolvedAccessor, val setter: ResolvedAccessor?)

    /**
     * Estrategias de generación de properties en Godot:
     *
     * 1. DIRECT: Getter/Setter existen como métodos reales en la clase.
     * 2. DELEGATED: * Getter/Setter existen en la clase padre (no en methods locales).
     * 3. INDEXED: * Property usa un método compartido con índice (ej: get_anchor(Side)).
     * 4. FALLBACK: * No existen métodos reales → generar TODO / acceso inseguro.
     *
     * **Nota**:
     * El JSON de Godot puede referenciar métodos inexistentes (_get/_set),
     * por lo que SIEMPRE se debe verificar contra methods reales.
     */
    context(_: Context)
    fun generateProperties(
        cls: ResolvedEngineClass,
        methods: List<EngineClass.ClassMethod>,
        classBuilder: TypeSpec.Builder,
    ): List<EngineClass.ClassMethod> {
        val usedMethodNames = mutableSetOf<String>()
        cls.raw.properties.forEach { property ->
            withExceptionContext({ "Error generating property '${property.name}'" }) {
                val resolved = resolvePropertyAccessors(
                    property,
                    methods,
                    cls,
                )

                if (resolved.getter.kind == AccessorKind.MISSING &&
                    (resolved.setter == null || resolved.setter.kind == AccessorKind.MISSING)
                ) {
                    /* MISSING
                    // FIXME Enable with logger debug
                    println(
                        buildString {
                            append("WARNING: Fallback generation for property ")
                            append(cls.name)
                            append(".")
                            append(property.name)
                            append(", ")
                            append("getter (expected: ")
                            append(property.getter)
                            append("): ")
                            append(resolved.getter.method?.name)
                            append(", strategy: ")
                            append(resolved.getter.kind)
                            if (property.setter != null) {
                                append(", setter (expected: ")
                                append(property.setter)
                                append("): ")
                                append(resolved.setter?.method?.name)
                                append(", strategy: ")
                                append(resolved.setter?.kind)
                            }
                        },
                    )
                     */
                    return@withExceptionContext
                }

                classBuilder.addProperty(
                    buildProperty(
                        property,
                        resolved,
                        cls,
                    ),
                )

                if (resolved.getter.kind == AccessorKind.LOCAL && property.index == null) {
                    resolved.getter.method?.name?.let { usedMethodNames.add(it) }
                }

                if (resolved.setter?.kind == AccessorKind.LOCAL && property.index == null) {
                    resolved.setter.method?.name?.let { usedMethodNames.add(it) }
                }
            }
        }
        return methods.filterNot { it.name in usedMethodNames }
    }

    // ===============================
    // Resolution Phase
    // ===============================

    context(_: Context)
    private fun resolvePropertyAccessors(
        property: EngineClass.ClassProperty,
        methods: List<EngineClass.ClassMethod>,
        engineClass: ResolvedEngineClass,
    ): ResolvedProperty {
        fun resolveSpecialSetter(name: String): ResolvedAccessor? = when {
            engineClass.name == "OptionButton" && property.name == "selected" && name == "_select_int" -> {
                val method = methods.find { it.name == "select" && it.arguments.size == 1 }
                method?.let { ResolvedAccessor(it, AccessorKind.LOCAL_DELEGATED) }
            }

            else -> null
        }

        fun resolve(name: String): ResolvedAccessor {
            resolveSpecialSetter(name)?.let { return it }

            val alt = name.removePrefix("_")

            // 1. LOCAL exact match (API real)
            methods.find { !it.isStatic && it.name == name }
                ?.let {
                    val kind = if (property.index != null) AccessorKind.INDEXED else AccessorKind.LOCAL

                    if (property.index == null && it.arguments.size > 1) {
                        return ResolvedAccessor(it, AccessorKind.LOCAL_DELEGATED)
                    }

                    return ResolvedAccessor(it, kind)
                }

            // 2. LOCAL via alt name (heurística → tratar como delegated)
            if (alt != name) {
                methods.find { !it.isStatic && it.name == alt }?.let {
                    val kind = if (property.index != null) {
                        AccessorKind.INDEXED
                    } else {
                        AccessorKind.LOCAL_DELEGATED
                    }

                    return ResolvedAccessor(it, kind)
                }
            }

            // 3. DELEGATED (padre)
            engineClass.allMethods
                .find { !it.isStatic && (it.name == name || it.name == alt) }
                ?.let {
                    val kind = if (property.index != null) AccessorKind.INDEXED else AccessorKind.DELEGATED
                    return ResolvedAccessor(it, kind)
                }

            return ResolvedAccessor(null, AccessorKind.MISSING)
        }

        val getter = resolve(property.getter)
        val setter = property.setter?.let { resolve(it) }

        return ResolvedProperty(getter, setter)
    }

    // ===============================
    // Build Phase
    // ===============================

    context(context: Context)
    private fun buildProperty(
        property: EngineClass.ClassProperty,
        resolved: ResolvedProperty,
        engineClass: ResolvedEngineClass,
    ): PropertySpec {
        val className = engineClass.name
        val kotlinName = safeIdentifier(property.name)

        val propertyType = resolved.getter.method?.returnValue?.let {
            typeResolver.resolve(it)
        } ?: resolved.setter?.method?.arguments?.lastOrNull()?.let {
            typeResolver.resolve(it)
        } ?: typeResolver.resolve(property.type)

        val builder = PropertySpec
            .builder(kotlinName, propertyType)
            .mutable(resolved.setter?.isCallableAsProperty == true)
            .experimentalApiAnnotation(className, property.name)
            .addKdocIfPresent(property)

        buildGetter(property, resolved.getter, builder, engineClass)

        if (resolved.setter?.isCallableAsProperty == true) {
            buildSetter(property, resolved.setter, builder, engineClass)
        }

        return builder.build()
    }

    // ===============================
    // Getter Builder
    // ===============================

    context(context: Context)
    private fun buildGetter(
        property: EngineClass.ClassProperty,
        accessor: ResolvedAccessor,
        builder: PropertySpec.Builder,
        engineClass: ResolvedEngineClass,
    ) {
        val method = accessor.method

        when (accessor.kind) {
            AccessorKind.MISSING -> {
                check(method == null) { "Missing getter requires a null method" }
                builder.getter(
                    BodyGenerator.todoGetter(
                        "Unknown getter for ${engineClass.name}.${property.name} (expected ${property.getter})",
                    ),
                )
            }

            AccessorKind.LOCAL, AccessorKind.LOCAL_DELEGATED -> {
                builder.getter(
                    FunSpec.getterBuilder()
                        .addCode(body.buildPropertyGetterBody(method!!, engineClass))
                        .build(),
                )
            }

            AccessorKind.DELEGATED -> {
                builder.getter(
                    FunSpec.getterBuilder()
                        .addModifiers(KModifier.INLINE)
                        .addStatement("return %N()", safeIdentifier(method!!.name))
                        .build(),
                )
            }

            AccessorKind.INDEXED -> {
                val enumConstant = resolveIndexedPropertyConstant(method!!, property.index!!)

                builder.getter(
                    FunSpec.getterBuilder()
                        .addModifiers(KModifier.INLINE)
                        .addStatement("return %N(%L)", safeIdentifier(method.name), enumConstant)
                        .build(),
                )
            }
        }
    }

    // ===============================
    // Setter Builder
    // ===============================

    context(context: Context)
    private fun buildSetter(
        property: EngineClass.ClassProperty,
        accessor: ResolvedAccessor,
        builder: PropertySpec.Builder,
        engineClass: ResolvedEngineClass,
    ) {
        val method = accessor.method

        when (accessor.kind) {
            AccessorKind.LOCAL -> {
                val arg = method!!.arguments.first()

                builder.setter(
                    FunSpec.setterBuilder()
                        .addParameter(safeIdentifier(arg.name), typeResolver.resolve(arg.type))
                        .addCode(body.buildPropertySetterBody(method, engineClass))
                        .build(),
                )
            }

            AccessorKind.LOCAL_DELEGATED -> {
                check(method != null && method.arguments.isNotEmpty()) {
                    "Local property setter requires a method with at least one argument"
                }
                val arg = method.arguments.find { it.type == property.type || it.meta == property.type }
                    ?: method.arguments.first()

                builder.setter(
                    FunSpec.setterBuilder()
                        .addModifiers(KModifier.INLINE)
                        .addParameter(safeIdentifier(arg.name), typeResolver.resolve(arg.type))
                        .addStatement("%N(%N)", safeIdentifier(method.name), safeIdentifier(arg.name))
                        .build(),
                )
            }

            AccessorKind.DELEGATED -> {
                check(method != null && method.arguments.size == 1) {
                    "Delegated property setter requires a method with exactly one argument"
                }
                val arg = method.arguments.first()

                builder.setter(
                    FunSpec.setterBuilder()
                        .addModifiers(KModifier.INLINE)
                        .addParameter(safeIdentifier(arg.name), typeResolver.resolve(arg.type))
                        .addStatement("%N(%N)", safeIdentifier(method.name), safeIdentifier(arg.name))
                        .build(),
                )
            }

            AccessorKind.INDEXED -> {
                check(method != null && method.arguments.size >= 2) {
                    "Indexed property setter requires a method with at least two argument"
                }
                val enumConstant = resolveIndexedPropertyConstant(method, property.index!!)
                val arg = method.arguments.last()

                builder.setter(
                    FunSpec.setterBuilder()
                        .addModifiers(KModifier.INLINE)
                        .addParameter(safeIdentifier(arg.name), typeResolver.resolve(arg.type))
                        .addStatement(
                            "%N(%L, %N)",
                            safeIdentifier(method.name),
                            enumConstant,
                            safeIdentifier(arg.name),
                        )
                        .build(),
                )
            }

            AccessorKind.MISSING -> {
                check(method == null) { "Missing setter requires a null method" }
                builder.setter(
                    FunSpec.setterBuilder()
                        .addParameter("value", typeResolver.resolve(property.type))
                        .addCode(
                            BodyGenerator.todoBody(
                                "Unknown setter for ${engineClass.name}.${property.name} (expected ${property.setter})",
                            ),
                        )
                        .build(),
                )
            }
        }
    }

    /**
     * Resuelve el índice numérico a una referencia de enum constant.
     * @param method Método getter/setter indexado
     * @param indexValue Valor numérico del índice
     * @return String del constant qualified (ej. "Flags.DISABLE_FOG") o el valor raw si no se encuentra
     */
    context(context: Context)
    private fun resolveIndexedPropertyConstant(method: EngineClass.ClassMethod, indexValue: Int): CodeBlock {
        check(method.arguments.isNotEmpty()) {
            "Indexed property getter/setter must have at least one argument, got ${method.arguments.size}"
        }
        val firstArg = method.arguments.first()

        // Fast path, the first argument is a primitive type
        if (firstArg.type in PRIMITIVE_NUMERIC_TYPES) {
            return CodeBlock.of("%L", indexValue)
        }

        val enumTypeStr = firstArg.type.removePrefix("enum::")
        var className: String? = null
        val enumName = if (enumTypeStr.contains(".")) {
            className = enumTypeStr.substringBeforeLast(".")
            enumTypeStr.substringAfterLast(".")
        } else {
            enumTypeStr
        }

        // Use resolveConstantUnambiguous here: the index of an indexed property is a raw integer
        // passed verbatim to the getter/setter, so an alias collision is worth logging — it doesn't
        // affect runtime correctness (all aliases have the same Long value) but it makes the choice
        // of emitted name explicit and reviewable during generation.
        val constantName = context.resolveEnumConstantUnambiguous(
            parentClass = className,
            enumName = enumName,
            value = indexValue.toLong(),
            context = "indexed property, method '${method.name}', index $indexValue",
        ) ?: error("Enum constant not found: $enumTypeStr.$indexValue, resolved from $className.$enumName")

        val enumTypeName = typeResolver.resolve(firstArg.type)
        return CodeBlock.of("%T.%L", enumTypeName, constantName)
    }
}
