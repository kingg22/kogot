package io.github.kingg22.godot.codegen.impl.runtime

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.joinToCode
import io.github.kingg22.godot.codegen.impl.buildKdoc
import io.github.kingg22.godot.codegen.impl.extensionapi.native.cstr
import io.github.kingg22.godot.codegen.impl.extensionapi.native.memScoped
import io.github.kingg22.godot.codegen.impl.extensionapi.native.ptr
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensioninterface.Arguments
import io.github.kingg22.godot.codegen.models.extensioninterface.Interface

class RuntimeFFIConvenienceGen {

    context(packageRegistry: RuntimePackageRegistry, resolver: RuntimeTypeResolver)
    fun buildConvenienceWrappers(iface: Interface): List<FunSpec> = buildList {
        buildTransformConvenienceWrapper(iface)?.let(::add)
        buildBooleanStatusConvenienceWrapper(iface)?.let(::add)
        buildCallErrorConvenienceWrapper(iface)?.let(::add)
    }

    context(_: RuntimePackageRegistry, _: RuntimeTypeResolver)
    private fun buildTransformConvenienceWrapper(iface: Interface): FunSpec? {
        val transforms = iface.arguments.mapIndexed { index, argument ->
            convenienceTransformFor(argument, index, iface.arguments)
        }
        val transformedReturn = convenienceReturnTypeFor(iface)
        val needsConvenience = transforms.any { it != null } || transformedReturn != null
        if (!needsConvenience) return null

        val convenienceParameters = buildList {
            transforms.forEachIndexed { index, transform ->
                when {
                    transform?.isSynthetic == true -> {}
                    transform?.parameter != null -> add(transform.parameter)
                    else -> add(buildParameter(iface.arguments[index], index))
                }
            }
        }

        return FunSpec
            .builder(memberName(iface))
            .addModifiers(KModifier.INLINE)
            .apply {
                convenienceParameters.forEach(::addParameter)
                transformedReturn?.let { returns(it) }

                addKdoc("Convenience wrapper for `%L`.", iface.name)
                val kdoc = buildKdoc(
                    description = iface.description,
                    see = iface.see,
                    since = iface.since,
                )
                if (kdoc.isNotBlank()) addKdoc("\n\n%L", kdoc)
                iface.deprecated?.let { addAnnotation(deprecatedAnnotation(it)) }
            }
            .addCode(buildConvenienceBody(iface, transforms, transformedReturn != null))
            .build()
    }

    context(
        _: RuntimeTypeResolver,
        packageRegistry: RuntimePackageRegistry,
    )
    private fun buildBooleanStatusConvenienceWrapper(iface: Interface): FunSpec? {
        val boolOuts = booleanOutputArguments(iface.arguments)
        if (boolOuts.isEmpty()) return null

        val transforms = iface.arguments.mapIndexed { index, argument ->
            if (index in boolOuts.map { it.index }.toSet()) {
                null
            } else {
                convenienceTransformFor(argument, index, iface.arguments)
            }
        }
        val parameters = buildList {
            transforms.forEachIndexed { index, transform ->
                if (index in boolOuts.map { it.index }.toSet()) return@forEachIndexed
                when {
                    transform?.isSynthetic == true -> {}
                    transform?.parameter != null -> add(transform.parameter)
                    else -> add(buildParameter(iface.arguments[index], index))
                }
            }
        }
        val rawReturnType = iface.returnValue?.type
        val hasBoolReturn = rawReturnType == "GDExtensionBool"
        val returnType = if (hasBoolReturn) {
            packageRegistry.booleanResultClassName()
        } else {
            packageRegistry.statusClassName()
        }

        return FunSpec
            .builder(memberName(iface))
            .addModifiers(KModifier.INLINE)
            .apply {
                parameters.forEach(::addParameter)
                returns(returnType)

                addKdoc("Status wrapper for `%L`.", iface.name)
                val kdoc = buildKdoc(
                    description = iface.description,
                    see = iface.see,
                    since = iface.since,
                )
                if (kdoc.isNotBlank()) addKdoc("\n\n%L", kdoc)
                iface.deprecated?.let { addAnnotation(deprecatedAnnotation(it)) }
            }
            .addCode(buildBooleanStatusBody(iface, transforms, boolOuts, hasBoolReturn))
            .build()
    }

    context(_: RuntimeTypeResolver, packageRegistry: RuntimePackageRegistry)
    private fun buildCallErrorConvenienceWrapper(iface: Interface): FunSpec? {
        val errorIndex = iface.arguments.indexOfFirst {
            parameterNameFor(it) == "rError" && it.type == "GDExtensionCallError*"
        }
        if (errorIndex == -1) return null

        val transforms = iface.arguments.mapIndexed { index, argument ->
            if (index == errorIndex) null else convenienceTransformFor(argument, index, iface.arguments)
        }
        val parameters = buildList {
            transforms.forEachIndexed { index, transform ->
                if (index == errorIndex) return@forEachIndexed
                when {
                    transform?.isSynthetic == true -> Unit
                    transform?.parameter != null -> add(transform.parameter)
                    else -> add(buildParameter(iface.arguments[index], index))
                }
            }
        }

        return FunSpec
            .builder(memberName(iface))
            .addModifiers(KModifier.INLINE)
            .apply {
                parameters.forEach(::addParameter)
                returns(packageRegistry.callErrorInfoClassName())

                addKdoc("Error snapshot wrapper for `%L`.", iface.name)
                val kdoc = buildKdoc(
                    description = iface.description,
                    see = iface.see,
                    since = iface.since,
                )
                if (kdoc.isNotBlank()) addKdoc("\n\n%L", kdoc)
                iface.deprecated?.let { addAnnotation(deprecatedAnnotation(it)) }
            }
            .addCode(buildCallErrorBody(iface, transforms, errorIndex))
            .build()
    }

    context(_: RuntimeTypeResolver)
    private fun buildConvenienceBody(
        iface: Interface,
        transforms: List<ConvenienceTransform?>,
        hasReturn: Boolean,
    ): CodeBlock {
        val rawName = rawWrapperName(iface)
        val requiresMemScoped = transforms.any { it?.requiresMemScoped == true }
        val invocationArgs = iface.arguments.mapIndexed { index, argument ->
            transforms[index]?.argumentExpression ?: CodeBlock.of("%N", buildParameter(argument, index).name)
        }
        val rawInvocation = CodeBlock.of("%N(%L)", rawName, invocationArgs.joinToCode())

        val bodyBuilder = CodeBlock.builder()
        if (requiresMemScoped) {
            bodyBuilder.beginControlFlow("%M", memScoped)
        }

        val boolReturn = iface.returnValue?.type == "GDExtensionBool"
        when {
            boolReturn -> bodyBuilder.addStatement("return %L.toBoolean()", rawInvocation)
            hasReturn -> bodyBuilder.addStatement("return %L", rawInvocation)
            else -> bodyBuilder.addStatement("%L", rawInvocation)
        }

        if (requiresMemScoped) {
            bodyBuilder.endControlFlow()
        }
        return bodyBuilder.build()
    }

    context(packageRegistry: RuntimePackageRegistry, _: RuntimeTypeResolver)
    private fun buildBooleanStatusBody(
        iface: Interface,
        transforms: List<ConvenienceTransform?>,
        boolOuts: List<BooleanOutput>,
        hasBooleanReturn: Boolean,
    ): CodeBlock {
        val rawName = rawWrapperName(iface)
        val omittedIndexes = boolOuts.map(BooleanOutput::index).toSet()
        val invocationArgs = iface.arguments.mapIndexed { index, argument ->
            when {
                index in omittedIndexes -> CodeBlock.of("%N", boolOuts.first { it.index == index }.localName)

                else -> transforms[index]?.argumentExpression
                    ?: CodeBlock.of("%N", buildParameter(argument, index).name)
            }
        }

        return CodeBlock.builder().apply {
            beginControlFlow("%M", memScoped)
            boolOuts.forEach { boolOut ->
                addStatement("val %N = %N()", boolOut.localName, MemberName(packageRegistry.rootPackage, "allocGdBool"))
            }
            if (hasBooleanReturn) {
                addStatement(
                    "val result = %N(%L).%M()",
                    rawName,
                    invocationArgs.joinToCode(),
                    MemberName(packageRegistry.rootPackage, "toBoolean"),
                )
                addStatement("return %T(", packageRegistry.classNameForOrDefault("BindingBooleanResult"))
                indent()
                addStatement("value = result,")
            } else {
                addStatement("%N(%L)", rawName, invocationArgs.joinToCode())
                addStatement("return %T(", packageRegistry.classNameForOrDefault("BindingStatus"))
                indent()
            }
            addStatement(
                "valid = %L,",
                boolOuts.find { it.kind == BooleanOutputKind.VALID }
                    ?.let {
                        CodeBlock.of(
                            "%N.%M()",
                            it.localName,
                            MemberName(packageRegistry.rootPackage, "readGdBool", true),
                        )
                    }
                    ?: "null",
            )
            addStatement(
                "outOfBounds = %L,",
                boolOuts.find { it.kind == BooleanOutputKind.OUT_OF_BOUNDS }
                    ?.let {
                        CodeBlock.of(
                            "%N.%M()",
                            it.localName,
                            MemberName(packageRegistry.rootPackage, "readGdBool", true),
                        )
                    }
                    ?: "null",
            )
            unindent()
            addStatement(")")
            endControlFlow()
        }.build()
    }

    context(packageRegistry: RuntimePackageRegistry, _: RuntimeTypeResolver)
    private fun buildCallErrorBody(
        iface: Interface,
        transforms: List<ConvenienceTransform?>,
        errorIndex: Int,
    ): CodeBlock {
        val rawName = rawWrapperName(iface)
        val invocationArgs = iface.arguments.mapIndexed { index, argument ->
            when {
                index == errorIndex -> CodeBlock.of("error")
                transforms[index]?.argumentExpression != null -> transforms[index]!!.argumentExpression
                else -> CodeBlock.of("%N", buildParameter(argument, index).name)
            }
        }

        return CodeBlock.builder().apply {
            beginControlFlow("%M", memScoped)
                .addStatement("val error = %M()", MemberName(packageRegistry.rootPackage, "allocCallError", true))
                .addStatement("%N(%L)", rawName, invocationArgs.joinToCode())
                .addStatement("return error.%M()", MemberName(packageRegistry.rootPackage, "readCallErrorInfo", true))
            endControlFlow()
        }.build()
    }

    context(packageRegistry: RuntimePackageRegistry, _: RuntimeTypeResolver)
    private fun convenienceTransformFor(
        argument: Arguments,
        index: Int,
        arguments: List<Arguments>,
    ): ConvenienceTransform? {
        fun safeArgName(argument: Arguments, index: Int) = safeIdentifier(
            argument.name
                ?.removePrefix("p")
                ?.removePrefix("_")
                ?.takeIf(String::isNotBlank)
                ?: "arg$index",
        )

        val name = safeArgName(argument, index)

        return when {
            argument.type == "GDExtensionBool" -> ConvenienceTransform(
                parameter = ParameterSpec
                    .builder(name, BOOLEAN)
                    .apply {
                        if (argument.description.isNotEmpty()) {
                            addKdoc("%L", argument.description.joinToString("\n"))
                        }
                    }
                    .build(),
                argumentExpression = CodeBlock.of(
                    "%N.%M()",
                    name,
                    MemberName(packageRegistry.rootPackage, "toGdBool", true),
                ),
            )

            argument.type == "const char*" -> ConvenienceTransform(
                parameter = ParameterSpec
                    .builder(name, STRING)
                    .apply {
                        if (argument.description.isNotEmpty()) {
                            addKdoc("%L", argument.description.joinToString("\n"))
                        }
                    }
                    .build(),
                argumentExpression = CodeBlock.of("%N.%M.%M", name, cstr, ptr),
                requiresMemScoped = true,
            )

            isPointerArrayWithCount(argument, index, arguments) -> {
                val elementType = pointerArrayElementType(argument.type)
                ConvenienceTransform(
                    parameter = ParameterSpec
                        .builder(name, elementType.copy(nullable = true))
                        .addModifiers(KModifier.VARARG)
                        .apply {
                            if (argument.description.isNotEmpty()) {
                                addKdoc("%L", argument.description.joinToString("\n"))
                            }
                        }
                        .build(),
                    argumentExpression = CodeBlock.of(
                        "%L(*%N)",
                        pointerArrayAllocator(argument.type),
                        name,
                    ),
                    requiresMemScoped = true,
                )
            }

            isCountForPointerArray(arguments, index) -> ConvenienceTransform(
                parameter = null,
                argumentExpression = CodeBlock.of(
                    "%L",
                    countExpression(
                        arguments[index].type,
                        safeArgName(arguments[index - 1], index - 1),
                    ),
                ),
                isSynthetic = true,
            )

            else -> null
        }
    }

    private fun convenienceReturnTypeFor(iface: Interface): TypeName? = when (iface.returnValue?.type) {
        "GDExtensionBool" -> BOOLEAN
        else -> null
    }

    private fun booleanOutputArguments(arguments: List<Arguments>) = arguments.mapIndexedNotNull { index, argument ->
        if (argument.type != "GDExtensionBool*") return@mapIndexedNotNull null
        when (parameterNameFor(argument)) {
            "rValid" -> BooleanOutput(index, "valid", BooleanOutputKind.VALID)
            "rOob" -> BooleanOutput(index, "outOfBounds", BooleanOutputKind.OUT_OF_BOUNDS)
            else -> null
        }
    }

    private fun parameterNameFor(argument: Arguments): String =
        safeIdentifier(argument.name?.takeIf(String::isNotBlank) ?: "")

    private fun isPointerArrayWithCount(argument: Arguments, index: Int, arguments: List<Arguments>): Boolean {
        if (index + 1 >= arguments.size) return false
        return pointerArrayAllocatorOrNull(argument.type) != null &&
            arguments[index + 1].name?.contains("count", ignoreCase = true) == true
    }

    private fun isCountForPointerArray(arguments: List<Arguments>, index: Int): Boolean =
        index > 0 && isPointerArrayWithCount(arguments[index - 1], index - 1, arguments)

    context(resolver: RuntimeTypeResolver)
    private fun pointerArrayElementType(rawType: String): TypeName = when (rawType) {
        "const GDExtensionConstVariantPtr*" -> resolver.resolveType("GDExtensionConstVariantPtr")
        "const GDExtensionConstTypePtr*" -> resolver.resolveType("GDExtensionConstTypePtr")
        else -> error("Unsupported convenience pointer array type: $rawType")
    }

    private fun pointerArrayAllocator(rawType: String): String =
        pointerArrayAllocatorOrNull(rawType) ?: error("Missing convenience allocator for $rawType")

    // region: FIXME use CodeBlock to always resolve imports and syntax
    private fun pointerArrayAllocatorOrNull(rawType: String): String? = when (rawType) {
        "const GDExtensionConstVariantPtr*" -> "allocConstVariantPtrArray"
        "const GDExtensionConstTypePtr*" -> "allocConstTypePtrArray"
        else -> null
    }

    private fun countExpression(countType: String, sourceName: String): String = when (countType) {
        "GDExtensionInt" -> "$sourceName.size.toLong()"
        "int32_t", "int" -> "$sourceName.size"
        else -> "$sourceName.size"
    }
    // endregion

    private data class ConvenienceTransform(
        val parameter: ParameterSpec?,
        val argumentExpression: CodeBlock,
        val requiresMemScoped: Boolean = false,
        val isSynthetic: Boolean = false,
    )

    private data class BooleanOutput(val index: Int, val localName: String, val kind: BooleanOutputKind)

    private enum class BooleanOutputKind { VALID, OUT_OF_BOUNDS, }
}
