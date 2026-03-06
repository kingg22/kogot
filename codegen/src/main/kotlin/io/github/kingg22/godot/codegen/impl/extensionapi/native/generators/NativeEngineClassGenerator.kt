package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.PRIMITIVE_NUMERIC_TYPES
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass

private val lazyMethod = MemberName("kotlin", "lazy")
private val lazyMode = ClassName("kotlin", "LazyThreadSafetyMode")

class NativeEngineClassGenerator(
    private val typeResolver: TypeResolver,
    private val body: BodyGenerator,
    private val methodGen: NativeMethodGenerator,
    private val enumGenerator: NativeEnumGenerator,
) {

    context(context: Context)
    fun generateFile(cls: EngineClass): FileSpec {
        val packageName = context.packageForOrDefault(cls.name)
        val spec = generateSpec(cls)
        return createFile(spec, spec.name!!, packageName)
    }

    context(context: Context)
    fun generateSpec(cls: EngineClass): TypeSpec {
        withExceptionContext({ "Generating class '${cls.name}'" }) {
            val classNameStr = cls.name.renameGodotClass()
            val packageName = context.packageForOrDefault(cls.name)
            val className = ClassName(packageName, classNameStr)
            val isSingleton = context.isSingleton(cls)
            val classBuilder = buildBaseClass(cls, className, isSingleton)

            val (staticMethods, instanceMethods) = cls.methods.partition { it.isStatic }

            // Sintetizar properties desde métodos getter/setter
            val standaloneMethods = generateProperties(cls, instanceMethods, classBuilder)

            // Métodos standalone (no forman parte de una property)
            standaloneMethods.forEach {
                val modifiers = Array(if (it.isVirtual) 1 else 0) { KModifier.OPEN }
                classBuilder.addFunction(methodGen.buildMethod(it, cls.name, *modifiers))
            }

            // Companion Object para Statics y Singleton Instance
            val companionBuilder = TypeSpec.companionObjectBuilder()

            if (isSingleton) {
                companionBuilder.addSingletonInstance(className)
            }

            cls.constants.forEach { constant ->
                withExceptionContext({ "Error generating class constant '${constant.name}'" }) {
                    companionBuilder.addProperty(
                        PropertySpec
                            .builder(constant.name, LONG, KModifier.CONST)
                            .initializer("%L", constant.value)
                            .addKdocIfPresent(constant)
                            .experimentalApiAnnotation(cls.name, constant.name)
                            .build(),
                    )
                }
            }

            staticMethods.forEach {
                companionBuilder.addFunction(methodGen.buildMethod(it, cls.name))
            }

            if (isSingleton || cls.constants.isNotEmpty() || staticMethods.isNotEmpty()) {
                classBuilder.addType(companionBuilder.build())
            }

            cls.enums.forEach { enum ->
                if (context.isSpecializedClass(enum.name)) return@forEach
                classBuilder.addType(enumGenerator.generateSpec(enum, cls.name))
            }

            return classBuilder.build()
        }
    }

    /**
     * Genera properties Kotlin desde cls.properties.
     *
     * @return Set de nombres de métodos no usados
     */
    context(context: Context)
    private fun generateProperties(
        cls: EngineClass,
        methods: List<EngineClass.ClassMethod>,
        classBuilder: TypeSpec.Builder,
    ): List<EngineClass.ClassMethod> {
        val usedMethodNames = mutableSetOf<String>()

        cls.properties.forEach { property ->
            withExceptionContext({ "Error generating property '${property.name}'" }) {
                val propertySpec = synthesizeProperty(property, methods, cls.name)
                classBuilder.addProperty(propertySpec)

                // Marcar getter/setter como usados solo si son properties standalone (sin delegación a methods)
                if (property.index == null) {
                    usedMethodNames.add(property.getter)
                    property.setter?.let { usedMethodNames.add(it) }
                }
            }
        }

        return methods.filterNot { it.name in usedMethodNames }
    }

    context(context: Context)
    private fun synthesizeProperty(
        property: EngineClass.ClassProperty,
        methods: List<EngineClass.ClassMethod>,
        className: String,
    ): PropertySpec {
        val safeName = safeIdentifier(property.name)

        // Buscar el método getter para obtener el tipo exacto
        val setterMethod = property.setter?.let { setter ->
            methods.find { it.name == setter }
        }

        val getterMethod = methods.find { it.name == property.getter }

        // Fallback generation when missing the method
        if (getterMethod == null || (property.setter != null && setterMethod == null)) {
            // FIXME: enable with logger.debug/verbose
            // println("INFO: Fallback generation for property $className.${property.name}")
            return generateProperty(property, className, getterMethod, setterMethod)
        }

        val returnValue = getterMethod.returnValue ?: error("Getter '${property.getter}' has no return type")

        // TypeResolver maneja el trato especial de type + meta
        val propertyType = typeResolver.resolve(returnValue)

        val propBuilder = PropertySpec
            .builder(safeName, propertyType)
            .mutable(property.setter != null)
            .experimentalApiAnnotation(className, property.name)
            .addKdocIfPresent(property)

        if (property.name != safeName) {
            propBuilder.addKdoc("\n\nOriginal name: `%S`", property.name)
        }

        if (property.type.contains(",")) {
            val (includedTypes, excludedTypes) = parsePropertyTypes(property.type)
            propBuilder.addKdoc("\n\nAccepts: %L", includedTypes.joinToString())
            if (excludedTypes.isNotEmpty()) {
                propBuilder.addKdoc("\n\nExcludes: %L", excludedTypes.joinToString())
            }
        }

        // Getter
        if (property.index != null) {
            // Buscar el tipo del parámetro del getter para saber qué enum usar
            val enumConstant = resolveIndexedPropertyConstant(
                method = getterMethod,
                indexValue = property.index,
            )

            propBuilder.getter(
                FunSpec
                    .getterBuilder()
                    .addStatement("return %N(%L)", safeIdentifier(property.getter), enumConstant)
                    .build(),
            )
        } else {
            propBuilder.getter(body.todoGetter())
        }

        // Setter
        if (property.setter != null) {
            if (property.index != null && setterMethod != null) {
                val enumConstant = resolveIndexedPropertyConstant(
                    method = setterMethod,
                    indexValue = property.index,
                )

                propBuilder.setter(
                    FunSpec
                        .setterBuilder()
                        .addParameter("value", propertyType)
                        .addStatement("%N(%L, value)", safeIdentifier(property.setter), enumConstant)
                        .build(),
                )
            } else {
                propBuilder.setter(
                    FunSpec
                        .setterBuilder()
                        .addParameter("value", propertyType)
                        .addCode(body.todoBody())
                        .build(),
                )
            }
        }

        return propBuilder.build()
    }

    /**
     * Resuelve el índice numérico a una referencia de enum constant.
     * @param method Método getter/setter indexado
     * @param indexValue Valor numérico del índice
     * @return String del constant qualified (ej. "Flags.DISABLE_FOG") o el valor raw si no se encuentra
     */
    context(context: Context)
    private fun resolveIndexedPropertyConstant(method: EngineClass.ClassMethod, indexValue: Int): CodeBlock {
        // El primer argumento del método es el enum
        check(method.arguments.isNotEmpty()) {
            "Indexed property getter/setter must have at least one argument, got ${method.arguments.size}"
        }
        val firstArg = method.arguments.first()

        // Fast path, the first argument is a primitive type
        if (firstArg.type in PRIMITIVE_NUMERIC_TYPES) {
            return CodeBlock.of(indexValue.toString())
        }

        val enumTypeStr = firstArg.type.removePrefix("enum::")

        var className: String? = null
        // Intentar resolver el constant
        val enumName = if (enumTypeStr.contains(".")) {
            className = enumTypeStr.substringBeforeLast(".")
            enumTypeStr.substringAfterLast(".")
        } else {
            enumTypeStr
        }

        val constantName = context.resolveEnumConstant(
            parentClass = className,
            enumName = enumName,
            value = indexValue.toLong(),
        ) ?: error("Enum constant not found: $enumTypeStr.$indexValue, resolved from $className.$enumName")

        val enumTypeName = typeResolver.resolve(firstArg.type)

        return CodeBlock.of("%T.%L", enumTypeName, constantName)
    }

    context(_: Context)
    private fun generateProperty(
        property: EngineClass.ClassProperty,
        className: String,
        getter: EngineClass.ClassMethod?,
        setter: EngineClass.ClassMethod?,
    ): PropertySpec {
        if (property.type.contains(',')) error("Multi-type properties are not supported by generate property yet")

        val memberType = if (getter != null && getter.returnValue != null) {
            typeResolver.resolve(getter.returnValue)
        } else {
            typeResolver.resolve(property.type)
        }

        val kotlinName = safeIdentifier(property.name)

        val propBuilder = PropertySpec
            .builder(kotlinName, memberType)
            .mutable(property.setter != null)
            .experimentalApiAnnotation(className, property.name)
            .addKdocIfPresent(property)

        if (property.name != kotlinName) {
            propBuilder.addKdoc("\n\nOriginal name: `%S`", property.name)
        }

        propBuilder.getter(
            FunSpec
                .getterBuilder()
                .addCode(body.todoBody())
                .build(),
        )

        if (property.setter != null) {
            val memberType = if (setter != null && setter.arguments.size == 1) {
                typeResolver.resolve(setter.arguments.first().type)
            } else {
                memberType
            }

            propBuilder.setter(
                FunSpec
                    .setterBuilder()
                    .addParameter("value", memberType)
                    .addCode(body.todoBody())
                    .build(),
            )
        }

        return propBuilder.build()
    }

    private data class PropertyTypes(val includedTypes: List<String>, val excludedTypes: List<String>)

    private fun parsePropertyTypes(multiType: String): PropertyTypes {
        val parts = multiType.split(",").map { it.trim() }
        return PropertyTypes(
            includedTypes = parts.filter { !it.startsWith("-") },
            excludedTypes = parts.filter { it.startsWith("-") }.map { it.removePrefix("-") },
        )
    }

    context(_: Context)
    private fun buildBaseClass(cls: EngineClass, className: ClassName, isSingleton: Boolean): TypeSpec.Builder {
        val builder = TypeSpec.classBuilder(className)
            .experimentalApiAnnotation(cls.name)
        val constructorSpec = FunSpec.constructorBuilder()

        // Modificadores de clase
        when {
            !cls.isInstantiable && !isSingleton -> builder.addModifiers(KModifier.ABSTRACT)

            isSingleton -> {
                // Final class, internal constructor
                if (cls.name == "PhysicsServer2D" || cls.name == "PhysicsServer3D") {
                    builder.addAnnotation(API_STATUS_NON_EXTENSIBLE)
                    builder.addModifiers(KModifier.OPEN)
                    constructorSpec.addModifiers(KModifier.INTERNAL)
                } else {
                    builder.addModifiers(KModifier.FINAL)
                    constructorSpec.addModifiers(KModifier.PRIVATE)
                }
            }

            else -> builder.addModifiers(KModifier.OPEN)
        }

        // Herencia
        cls.inherits?.let { superClassType ->
            builder.superclass(typeResolver.resolve(superClassType))
        }

        return builder
            .primaryConstructor(constructorSpec.build())
            .addKdocIfPresent(cls)
    }

    /** @receiver Companion Builder */
    private fun TypeSpec.Builder.addSingletonInstance(className: ClassName) = apply {
        addProperty(
            PropertySpec
                .builder("instance", className)
                .delegate(
                    CodeBlock
                        .builder()
                        .beginControlFlow("%M(%T.NONE)", lazyMethod, lazyMode)
                        .addStatement("%T()", className)
                        .endControlFlow()
                        .build(),
                )
                .build(),
        )
    }
}
