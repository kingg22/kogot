package io.github.kingg22.godot.codegen.impl.runtime

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.K_OPT_IN
import io.github.kingg22.godot.codegen.impl.K_SUPPRESS
import io.github.kingg22.godot.codegen.impl.buildKdoc
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.cinteropCstr
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.cinteropInvoke
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.cinteropPtr
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.cinteropReinterpret
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.lazyMethod
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.memScoped
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.models.extensioninterface.Interface

class RuntimeFFIGenerator(private val packageName: String) {
    private val convenienceGen = RuntimeFFIConvenienceGen()

    fun generate(interfaceModel: GDExtensionInterface): Sequence<FileSpec> {
        val packageRegistry = RuntimePackageRegistry(packageName, interfaceModel)
        val resolver = RuntimeTypeResolver(interfaceModel, packageName)

        return interfaceModel.interfaces
            .groupBy { prefixOf(it) }
            .toSortedMap()
            .asSequence()
            .map { (prefix, interfaces) ->
                context(packageRegistry, resolver) {
                    generateGroupFile(prefix, interfaces)
                }
            }
    }

    context(packageRegistry: RuntimePackageRegistry, resolver: RuntimeTypeResolver)
    private fun generateGroupFile(prefix: String, interfaces: List<Interface>): FileSpec {
        val className = packageRegistry.bindingClassName(prefix)
        val constructorParam = ParameterSpec
            .builder(
                "getProcAddress",
                resolver.resolveType("GDExtensionInterfaceGetProcAddress").copy(nullable = false),
            )
            .build()

        val typeSpec = TypeSpec
            .classBuilder(className)
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addParameter(constructorParam)
                    .addModifiers(KModifier.PRIVATE)
                    .build(),
            )
            .addKdoc(
                "Runtime bindings for `%L_*` symbols loaded through `getProcAddress`.\n\nGenerated from Godot 4.6.1 `gdextension_interface.json`.",
                prefix,
            )
            .addAnnotation(
                AnnotationSpec
                    .builder(K_SUPPRESS)
                    .addMember("%S", "DEPRECATION")
                    .addMember("%S", "DEPRECATION_ERROR")
                    .addMember("%S", "NOTHING_TO_INLINE")
                    .build(),
            )
            .addAnnotation(
                AnnotationSpec
                    .builder(K_OPT_IN)
                    .addMember("%T::class", packageRegistry.classNameForOrDefault("InternalBinding"))
                    .build(),
            )
            .addType(buildCompanionObject(className))
            .apply {
                interfaces.forEach { iface ->
                    addProperty(buildFunctionPointerProperty(iface))
                    addFunction(buildRawWrapper(iface))
                    convenienceGen.buildConvenienceWrappers(iface).forEach(::addFunction)
                }
            }
            .build()

        return createFile(typeSpec, className.simpleName, packageRegistry.rootPackage)
    }

    context(packageRegistry: RuntimePackageRegistry)
    private fun buildCompanionObject(className: ClassName): TypeSpec = TypeSpec
        .companionObjectBuilder()
        .addProperty(
            PropertySpec
                .builder("instance", className)
                .delegate(
                    CodeBlock
                        .builder()
                        .beginControlFlow("%M(PUBLICATION)", lazyMethod)
                        .addStatement(
                            "%T(%T.getProcAddress)",
                            className,
                            packageRegistry.classNameForOrDefault("BindingProcAddressHolder"),
                        )
                        .endControlFlow()
                        .build(),
                )
                .build(),
        )
        .build()

    context(packageRegistry: RuntimePackageRegistry, resolver: RuntimeTypeResolver)
    private fun buildFunctionPointerProperty(iface: Interface): PropertySpec {
        val aliasType = resolver.resolveInterfaceAlias(iface)
        val propertyName = functionPointerPropertyName(iface)

        return PropertySpec
            .builder(propertyName, aliasType)
            .apply {
                addKdoc("Low-level pointer for `%S`.", iface.name)
                iface.deprecated?.let { addAnnotation(deprecatedAnnotation(it)) }
            }
            .addAnnotation(packageRegistry.classNameForOrDefault("InternalBinding"))
            .delegate(
                CodeBlock
                    .builder()
                    .beginControlFlow("%M(PUBLICATION)", lazyMethod)
                    .add(
                        CodeBlock
                            .builder()
                            .beginControlFlow("%M", memScoped)
                            .addStatement("getProcAddress(%S.%M.%M)", iface.name, cinteropCstr, cinteropPtr)
                            .indent()
                            .addStatement("?.%M()", cinteropReinterpret)
                            .addStatement("?: error(%S)", "Failed to load Godot symbol '${iface.name}'")
                            .unindent()
                            .endControlFlow()
                            .build(),
                    )
                    .endControlFlow()
                    .build(),
            )
            .build()
    }

    context(resolver: RuntimeTypeResolver)
    private fun buildRawWrapper(iface: Interface): FunSpec {
        val propertyName = functionPointerPropertyName(iface)
        val parameters = iface.arguments.mapIndexed { index, argument ->
            buildParameter(argument, index)
        }
        val hasReturn = iface.returnValue != null && iface.returnValue.type != "void"

        return FunSpec
            .builder(rawWrapperName(iface))
            .addModifiers(KModifier.INLINE)
            .apply {
                parameters.forEach(::addParameter)
                if (hasReturn) {
                    returns(resolver.resolveReturnType(iface.returnValue))
                }

                addKdoc("C symbol: `%L`.", iface.name)
                val kdoc = buildKdoc(
                    description = iface.description,
                    see = iface.see,
                    since = iface.since,
                )
                if (kdoc.isNotBlank()) {
                    addKdoc("\n\n%L", kdoc)
                }
                iface.deprecated?.let { addAnnotation(deprecatedAnnotation(it)) }
            }
            .addCode(
                buildInvokeCode(
                    propertyName = propertyName,
                    parameterNames = parameters.map(ParameterSpec::name),
                    hasReturn = hasReturn,
                ),
            )
            .build()
    }

    private fun buildInvokeCode(propertyName: String, parameterNames: List<String>, hasReturn: Boolean): CodeBlock {
        val invocation = CodeBlock.of(
            "%N.%M(%L)",
            propertyName,
            cinteropInvoke,
            parameterNames.joinToString(", "),
        )

        return CodeBlock.builder().apply {
            if (hasReturn) {
                addStatement("return %L", invocation)
            } else {
                addStatement("%L", invocation)
            }
        }.build()
    }
}
