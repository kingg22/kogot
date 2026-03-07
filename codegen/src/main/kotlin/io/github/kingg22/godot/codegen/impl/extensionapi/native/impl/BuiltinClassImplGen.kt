package io.github.kingg22.godot.codegen.impl.extensionapi.native.impl

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.native.BYTE_VAR
import io.github.kingg22.godot.codegen.impl.extensionapi.native.COPAQUE_POINTER
import io.github.kingg22.godot.codegen.impl.extensionapi.native.C_POINTER
import io.github.kingg22.godot.codegen.impl.extensionapi.native.cinteropInvoke
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.BodyGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.memScoped
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinConstructor
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface

private const val VARIANT_TYPE_STRING = "GDEXTENSION_VARIANT_TYPE_STRING"
private const val VARIANT_TYPE_STRING_NAME = "GDEXTENSION_VARIANT_TYPE_STRING_NAME"
private const val VARIANT_TYPE_NODE_PATH = "GDEXTENSION_VARIANT_TYPE_NODE_PATH"
private val STORAGE_BACKED_BUILTINS = setOf("String", "StringName", "NodePath")

class BuiltinClassImplGen(private val delegate: BodyGenerator) {
    private lateinit var implPackageRegistry: ImplementationPackageRegistry

    fun todoBody() = delegate.todoBody()
    fun todoGetter() = delegate.todoGetter()

    fun initialize(packageStr: String, gdInterface: GDExtensionInterface) {
        implPackageRegistry = ImplementationPackageRegistry(packageStr, gdInterface)
    }

    fun configureStorageBackedBuiltin(
        builtinClass: ResolvedBuiltinClass,
        classBuilder: TypeSpec.Builder,
    ): TypeSpec.Builder = classBuilder.apply {
        if (builtinClass.name !in STORAGE_BACKED_BUILTINS) return@apply

        val storageType = C_POINTER.parameterizedBy(BYTE_VAR)
        val storageProperty = PropertySpec
            .builder("storage", storageType, KModifier.PRIVATE)
            .initializer("storage")
            .build()

        classBuilder.primaryConstructor(
            FunSpec
                .constructorBuilder()
                .addParameter("storage", storageType)
                .addModifiers(KModifier.PRIVATE)
                .build(),
        )
        classBuilder.addProperty(storageProperty)
        classBuilder.addProperty(
            PropertySpec
                .builder("closed", BOOLEAN, KModifier.PRIVATE)
                .mutable(true)
                .initializer("false")
                .build(),
        )
        classBuilder.addProperty(
            PropertySpec
                .builder("rawPtr", COPAQUE_POINTER, KModifier.INTERNAL)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addStatement("return %N", storageProperty)
                        .build(),
                )
                .build(),
        )
    }

    fun buildCloseFunction(builtinClass: ResolvedBuiltinClass): FunSpec {
        if (builtinClass.name !in STORAGE_BACKED_BUILTINS) {
            return FunSpec
                .builder("close")
                .addModifiers(KModifier.OVERRIDE)
                .addCode(todoBody())
                .build()
        }

        return FunSpec
            .builder("close")
            .addModifiers(KModifier.OVERRIDE)
            .addCode(
                CodeBlock.builder()
                    .beginControlFlow("if (!closed)")
                    .add(destroyCallFor(builtinClass))
                    .addStatement("%M(%N)", implPackageRegistry.memberNameForOrDefault("freeBuiltinStorage"), "storage")
                    .addStatement("closed = true")
                    .endControlFlow()
                    .build(),
            )
            .build()
    }

    context(_: Context)
    fun constructorBodyFor(
        builtinClass: ResolvedBuiltinClass,
        ctor: ResolvedBuiltinConstructor,
        ctorBuilder: FunSpec.Builder,
    ): CodeBlock {
        if (builtinClass.name !in STORAGE_BACKED_BUILTINS) return todoBody()

        ctorBuilder.callThisConstructor(
            CodeBlock.of(
                "%M(%L)",
                implPackageRegistry.memberNameForOrDefault("allocateBuiltinStorage"),
                builtinStorageSize(builtinClass),
            ),
        )

        return CodeBlock
            .builder()
            .add(constructorInvocation(builtinClass, ctor))
            .build()
    }

    context(context: Context)
    fun stringConstructorBodyFor(builtinClass: ResolvedBuiltinClass, ctorBuilder: FunSpec.Builder): CodeBlock {
        if (builtinClass.name in STORAGE_BACKED_BUILTINS) {
            ctorBuilder.callThisConstructor(
                CodeBlock.of(
                    "%M(%L)",
                    implPackageRegistry.memberNameForOrDefault("allocateBuiltinStorage"),
                    builtinStorageSize(builtinClass),
                ),
            )
        }

        return when (builtinClass.name) {
            "String" -> CodeBlock.builder()
                .addStatement(
                    "%T.instance.newWithUtf8Chars(rawPtr, value)",
                    implPackageRegistry.classNameForOrDefault("StringBinding"),
                )
                .build()

            "StringName" -> CodeBlock.builder()
                .addStatement(
                    "%T.instance.nameNewWithUtf8Chars(rawPtr, value)",
                    implPackageRegistry.classNameForOrDefault("StringBinding"),
                )
                .build()

            "NodePath" -> CodeBlock.builder()
                .beginControlFlow(
                    "%T(value).use { godotString ->",
                    context.classNameForOrDefault("String", "GodotString"),
                )
                .add(callBuiltinConstructor(VARIANT_TYPE_NODE_PATH, 2, "godotString.rawPtr"))
                .endControlFlow()
                .build()

            else -> todoBody()
        }
    }

    fun destroyCallFor(builtinClass: ResolvedBuiltinClass): CodeBlock = when (builtinClass.name) {
        "String" -> destroyBuiltin(VARIANT_TYPE_STRING)
        "StringName" -> destroyBuiltin(VARIANT_TYPE_STRING_NAME)
        "NodePath" -> destroyBuiltin(VARIANT_TYPE_NODE_PATH)
        else -> error("Missing destroy method for ${builtinClass.name}")
    }

    private fun builtinStorageSize(builtinClass: ResolvedBuiltinClass): Int = builtinClass.layout?.size
        ?: error("Missing layout size for ${builtinClass.name}")

    context(context: Context)
    private fun constructorInvocation(builtinClass: ResolvedBuiltinClass, ctor: ResolvedBuiltinConstructor): CodeBlock =
        when {
            ctor.usesKotlinStringBridge -> when (builtinClass.name) {
                "String" -> CodeBlock.of(
                    "%T.instance.newWithUtf8Chars(rawPtr, value)",
                    implPackageRegistry.classNameForOrDefault("StringBinding"),
                )

                "StringName" -> CodeBlock.of(
                    "%T.instance.nameNewWithUtf8Chars(rawPtr, value)",
                    implPackageRegistry.classNameForOrDefault("StringBinding"),
                )

                "NodePath" -> CodeBlock.builder()
                    .beginControlFlow(
                        "%T(value).use { godotString ->",
                        context.classNameForOrDefault("String", "GodotString"),
                    )
                    .add(callBuiltinConstructor(VARIANT_TYPE_NODE_PATH, 2, "godotString.rawPtr"))
                    .endControlFlow()
                    .build()

                else -> todoBody()
            }

            builtinClass.name == "String" -> when (ctor.index) {
                0 -> callBuiltinConstructor(VARIANT_TYPE_STRING, ctor.index)
                1 -> callBuiltinConstructor(VARIANT_TYPE_STRING, ctor.index, "from.rawPtr")
                2 -> callBuiltinConstructor(VARIANT_TYPE_STRING, ctor.index, "from.rawPtr")
                3 -> callBuiltinConstructor(VARIANT_TYPE_STRING, ctor.index, "from.rawPtr")
                else -> todoBody()
            }

            builtinClass.name == "StringName" -> when (ctor.index) {
                0 -> callBuiltinConstructor(VARIANT_TYPE_STRING_NAME, ctor.index)
                1 -> callBuiltinConstructor(VARIANT_TYPE_STRING_NAME, ctor.index, "from.rawPtr")
                2 -> callBuiltinConstructor(VARIANT_TYPE_STRING_NAME, ctor.index, "from.rawPtr")
                else -> todoBody()
            }

            builtinClass.name == "NodePath" -> when (ctor.index) {
                0 -> callBuiltinConstructor(VARIANT_TYPE_NODE_PATH, ctor.index)
                1 -> callBuiltinConstructor(VARIANT_TYPE_NODE_PATH, ctor.index, "from.rawPtr")
                2 -> callBuiltinConstructor(VARIANT_TYPE_NODE_PATH, ctor.index, "from.rawPtr")
                else -> todoBody()
            }

            else -> todoBody()
        }

    private fun callBuiltinConstructor(variantType: String, constructorIndex: Int, vararg args: String): CodeBlock =
        CodeBlock
            .builder()
            .beginControlFlow("%M", memScoped)
            .addStatement(
                "val constructor = %T.instance.getPtrConstructorRaw(%N, %L)",
                implPackageRegistry.classNameForOrDefault("VariantBinding"),
                variantType,
                constructorIndex,
            ).indent()
            .addStatement("?: error(%S)", "Missing builtin constructor")
            .unindent()
            .addStatement(
                "constructor.%M(rawPtr, %M(%L))",
                cinteropInvoke,
                implPackageRegistry.memberNameForOrDefault("allocConstTypePtrArray"),
                args.joinToString(),
            )
            .endControlFlow()
            .build()

    private fun destroyBuiltin(variantType: String): CodeBlock = CodeBlock
        .builder()
        .addStatement(
            "val %N = %T.instance.getPtrDestructorRaw(%N)",
            "destructor",
            implPackageRegistry.classNameForOrDefault("VariantBinding"),
            variantType,
        ).indent()
        .addStatement("?: error(%S)", "Missing builtin destructor")
        .unindent()
        .addStatement("%N.%M(rawPtr)", "destructor", cinteropInvoke)
        .build()
}
