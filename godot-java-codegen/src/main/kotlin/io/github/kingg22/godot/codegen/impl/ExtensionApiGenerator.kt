package io.github.kingg22.godot.codegen.impl

import com.squareup.kotlinpoet.*
import io.github.kingg22.godot.codegen.models.extensionapi.ApiEnum
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinEnum
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensionapi.GodotClass
import io.github.kingg22.godot.codegen.models.extensionapi.NativeStructure
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction
import java.nio.file.Path

private const val TYPE_PREFIX = "GD"
private val MAPPED_GODOT_BUILTIN_CLASSES = setOf(
    "int",
    "long",
    "float",
    "double",
    "bool",
    "nil",
)

class ExtensionApiGenerator(private val packageName: String) {
    fun generate(api: ExtensionApi, outputDir: Path): List<Path> {
        val size = (
            api.globalEnums.size +
                api.builtinClasses.size +
                1 + // variant class
                api.classes.size +
                api.nativeStructures.size +
                1 // utility functions
            )
        val singletons = api.singletons.map { it.name }

        val builtinClasses = api.builtinClasses.asSequence().mapNotNull { clazz ->
            generateBuiltinClass(clazz, singletons)?.writeTo(outputDir)
        }

        val classes = api.classes.asSequence().map { clazz ->
            generateClass(clazz, singletons).writeTo(outputDir)
        }

        if (api.globalConstants.isNotEmpty()) {
            System.err.println(
                "WARNING: Global constants are not supported yet. Found: [${api.globalConstants.joinToString()}]",
            )
        }

        val (nestedEnum, globalEnums) = api.globalEnums.partition { it.name.contains(".") }

        if (nestedEnum.size > 2) {
            System.err.println(
                "WARNING: Nested enums (${nestedEnum.size}) [" + nestedEnum.joinToString(postfix = "]") { it.name },
            )
        }

        val enums = globalEnums.asSequence().map { enumDef ->
            val enumSpec = generateEnum(enumDef)
            createFile(enumSpec, enumDef.name).writeTo(outputDir)
        }

        val variantClass = generateVariant(nestedEnum).writeTo(outputDir)

        val utilityFunctions = generateUtilityFunctions(api.utilityFunctions).writeTo(outputDir)

        val nativeStructures = api.nativeStructures.asSequence().map { ns ->
            generateNativeStructure(ns).writeTo(outputDir)
        }

        return ArrayList<Path>(size).apply {
            addAll(builtinClasses)
            addAll(classes)
            addAll(enums)
            add(variantClass)
            add(utilityFunctions)
            addAll(nativeStructures)
        }
    }

    private fun createFile(type: TypeSpec, fileName: String): FileSpec = FileSpec
        .builder(packageName, fileName)
        .commonConfiguration()
        .addType(type)
        .build()

    private fun generateEnum(enumDef: ApiEnum): TypeSpec {
        val typeBuilder = TypeSpec.enumBuilder(enumDef.name)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", LONG)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("value", LONG)
                    .initializer("value")
                    .build(),
            )

        enumDef.values.forEach { value ->
            val enumConst = TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%L", value.value)
                .build()
            typeBuilder.addEnumConstant(sanitizeEnumConstant(value.name), enumConst)
        }
        return typeBuilder.build()
    }

    private fun generateBuiltinClass(cls: BuiltinClass, singletons: List<String>): FileSpec? {
        // TODO generate special GodotX utilities (operators, constructor, mapper, etc)
        if (cls.name.lowercase() in MAPPED_GODOT_BUILTIN_CLASSES) return null

        fun BuiltinClass.isSingleton(): Boolean = singletons.any { it == this.name }

        val className = if (cls.name == "String") {
            "GodotString"
        } else {
            cls.name
        }

        enrichExceptions({ "Generating builtin class '$className', isSingleton: ${cls.isSingleton()}" }) {
            val typeBuilder = generateClass(className, null, cls.isSingleton())
            val companionObject = typeBuilder.typeSpecs
                .find { it.isCompanion }?.also { typeBuilder.typeSpecs.remove(it) }?.toBuilder()
                ?: TypeSpec.companionObjectBuilder()

            // Métodos
            cls.methods.forEach { method ->
                enrichExceptions({ "Error generating function '${method.name}', type: ${method.returnType}" }) {
                    val methodReturnType = method.returnType?.let { typeNameFor(packageName, it) } ?: UNIT
                    val funSpec = generateMethod(
                        method.name,
                        method.returnType,
                        methodReturnType,
                        isOpen = !cls.isSingleton() && !method.isStatic,
                    ) {
                        enrichExceptions({
                            "Generating parameters: [${
                                method.arguments.joinToString { "Name: ${it.name}, type: ${it.type}" }
                            }]"
                        }) {
                            methodArgsToParameters(packageName, method.isVararg, method.arguments)
                        }
                    }

                    if (method.isStatic) {
                        companionObject.addFunction(funSpec.addAnnotation(jvmStaticAnnotation()).build())
                    } else {
                        typeBuilder.addFunction(funSpec.build())
                    }
                }
            }

            fun BuiltinEnum.asApiEnum() = ApiEnum(name = name, isBitfield = false, values = values)

            if (companionObject.build() != TypeSpec.companionObjectBuilder().build()) {
                typeBuilder.typeSpecs.addFirst(companionObject.build())
            }

            val enums = cls.enums.map { enumDef ->
                generateEnum(enumDef.asApiEnum())
            }
            typeBuilder.addTypes(enums)

            return createFile(typeBuilder.build(), className)
        }
    }

    private fun generateClass(cls: GodotClass, singletons: List<String>): FileSpec {
        fun GodotClass.isSingleton(): Boolean = singletons.any { it == this.name }

        enrichExceptions({ "Generating class '${cls.name}', isSingleton: ${cls.isSingleton()}" }) {
            val parent = cls.inherits?.takeIf { it.isNotBlank() }
            val parentClass = parent?.let { typeNameFor(packageName, parent) }
            val typeBuilder = generateClass(cls.name, parentClass, cls.isSingleton())
            val companionObject = typeBuilder.typeSpecs
                .find { it.isCompanion }?.also { typeBuilder.typeSpecs.remove(it) }?.toBuilder()
                ?: TypeSpec.companionObjectBuilder()

            cls.methods.forEach { method ->
                enrichExceptions({ "Error generating function '${method.name}', type: ${method.returnValue?.type}" }) {
                    val methodReturnType = methodReturnTypeName(packageName, method.returnValue)

                    val funSpec = generateMethod(
                        method.name,
                        method.returnValue?.type,
                        methodReturnType,
                        isOpen = !cls.isSingleton() && !method.isStatic,
                    ) {
                        enrichExceptions({
                            "Generating parameters: [${
                                method.arguments.joinToString {
                                    "Name: ${it.name}, type: ${it.type}"
                                }
                            }]"
                        }) {
                            methodArgsToParameters(packageName, method.isVararg, method.arguments)
                        }
                    }

                    if (method.isStatic) {
                        companionObject.addFunction(funSpec.addAnnotation(jvmStaticAnnotation()).build())
                    } else {
                        typeBuilder.addFunction(funSpec.build())
                    }
                }
            }

            if (companionObject.build() != TypeSpec.companionObjectBuilder().build()) {
                typeBuilder.typeSpecs.addFirst(companionObject.build())
            }
            val enumSpecs = cls.enums.map { enumDef -> generateEnum(enumDef) }
            typeBuilder.addTypes(enumSpecs)

            return createFile(typeBuilder.build(), cls.name)
        }
    }

    private fun generateNativeStructure(ns: NativeStructure): FileSpec {
        val typeBuilder = TypeSpec.classBuilder(ns.name).addModifiers(KModifier.OPEN)
        return createFile(typeBuilder.build(), ns.name)
    }

    private fun generateMethod(
        name: String,
        returnTypeString: String?,
        returnType: TypeName,
        isOpen: Boolean,
        arguments: () -> List<ParameterSpec>,
    ): FunSpec.Builder {
        val methodName = safeIdentifier(name)
        val funSpec = FunSpec.builder(methodName)
            .addParameters(arguments())
            .returns(returnType)
            .addKdocForBitfield(returnTypeString, "@return")
            .addStatement("TODO()")
            .accidentalOverride(methodName, returnType)
        if (isOpen) funSpec.addModifiers(KModifier.OPEN)
        return funSpec
    }

    private fun generateUtilityFunctions(functions: List<UtilityFunction>): FileSpec {
        enrichExceptions({ "Generating utility functions, count: ${functions.size}" }) {
            val typeBuilder = TypeSpec
                .objectBuilder("GD")
                .addKdoc("Utility functions for Godot API.")

            functions.forEach { method ->
                enrichExceptions({ "Error generating function '${method.name}', type: ${method.returnType}" }) {
                    val methodReturnType = method.returnType?.let { typeNameFor(packageName, it) } ?: UNIT
                    val funSpec = generateMethod(
                        method.name,
                        method.returnType,
                        methodReturnType,
                        isOpen = false,
                    ) {
                        enrichExceptions({
                            "Generating parameters: [${
                                method.arguments.joinToString {
                                    "Name: ${it.name}, type: ${it.type}"
                                }
                            }]"
                        }) {
                            methodArgsToParameters(packageName, method.isVararg, method.arguments)
                        }
                    }.addKdoc("Category: %L", method.category).build()
                    typeBuilder.addFunction(funSpec)
                }
            }

            return createFile(typeBuilder.build(), "GD")
        }
    }

    /** enums internos vendrán de globalEnums Variant. */
    private fun generateVariant(enums: List<ApiEnum>): FileSpec {
        enrichExceptions({ "Generating Variant class, nested enums count: ${enums.size}" }) {
            val typeBuilder = TypeSpec
                .classBuilder("Variant")
                .addModifiers(KModifier.SEALED)
                .addTypes(
                    enums.map {
                        enrichExceptions({ "Error generating nested enum '${it.name}'" }) {
                            generateEnum(it.copy(name = it.name.substringAfterLast(".")))
                        }
                    },
                )
            // TODO generate nested class of Variant.Type
            // TODO add operators
            return createFile(typeBuilder.build(), "Variant")
        }
    }

    private fun generateClass(className: String, baseClass: TypeName?, isSingleton: Boolean): TypeSpec.Builder =
        if (isSingleton) {
            generateSingletonClass(className, baseClass)
        } else {
            generateRegularClass(className, baseClass)
        }

    private fun generateSingletonClass(className: String, baseClass: TypeName?): TypeSpec.Builder {
        val classType = ClassName(packageName, className)

        val lazyMethod = MemberName("kotlin", "lazy")

        val lazyMode = ClassName("kotlin", "LazyThreadSafetyMode")

        val companion = TypeSpec
            .companionObjectBuilder()
            .addProperty(
                PropertySpec
                    .builder("instance", classType)
                    .addAnnotation(
                        jvmNameAnnotation("instance")
                            .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                            .build(),
                    )
                    .delegate(
                        CodeBlock
                            .builder()
                            .beginControlFlow("%M(%T.NONE)", lazyMethod, lazyMode)
                            .addStatement("%T()", classType)
                            .endControlFlow()
                            .build(),
                    )
                    .build(),
            )
            .build()

        // Make open and protected constructor because some singletons are base for extensions
        // TODO mark as @ApiStatus.NoExtensible or something else
        val classSpec = TypeSpec
            .classBuilder(classType)
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(
                // TODO
                FunSpec
                    .constructorBuilder()
                    .addModifiers(KModifier.PROTECTED)
                    .build(),
            )
            .addType(companion)
        baseClass?.let { classSpec.superclass(it) }

        return classSpec
    }

    private fun generateRegularClass(
        className: String,
        baseClass: TypeName?,
        modifiers: List<KModifier> = listOf(KModifier.OPEN),
    ): TypeSpec.Builder = TypeSpec.classBuilder(className)
        .addModifiers(modifiers).apply { baseClass?.let { superclass(it) } }

    private fun FunSpec.Builder.accidentalOverride(funName: String, returnType: TypeName): FunSpec.Builder =
        if (funName == "wait" && returnType == UNIT) {
            System.err.println("INFO: modifying wait() to avoid conflicts with JVM Object method")
            this
                .addKdoc(
                    "Generated Note: Original name was `wait`, renamed to avoid conflicts with JVM [java.lang.Object] method.",
                )
                .build()
                .toBuilder("await")
        } else {
            this
        }

    private inline fun <T> enrichExceptions(metadata: () -> String, block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        throw RuntimeException(metadata(), e)
    }
}
