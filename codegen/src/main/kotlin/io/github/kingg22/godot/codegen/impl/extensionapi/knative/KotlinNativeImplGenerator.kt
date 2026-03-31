package io.github.kingg22.godot.codegen.impl.extensionapi.knative

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.generators.*
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl.BuiltinClassImplGen
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl.BuiltinMethodImplGen
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl.EngineClassImplGen
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl.EngineMethodImplGen
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl.ImplementationPackageRegistry
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl.UtilityFunctionImplGen
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl.VariantImplGen
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi

/** Generates Kotlin Native implementation bodies (cinterop / GDExtension bindings). */
class KotlinNativeImplGenerator(override val typeResolver: TypeResolver) : CodeImplGenerator.ImplGenerator {
    private lateinit var implPackageRegistry: ImplementationPackageRegistry
    private val bodyGenerator = BodyGenerator
    private val builtinClassImplGen = BuiltinClassImplGen(typeResolver, BuiltinMethodImplGen(typeResolver))
    private val defaultValue = DefaultValueGenerator(typeResolver)
    private val methodGenerator = NativeMethodGenerator(typeResolver, bodyGenerator, defaultValue)
    private val genericInterceptor = GenericBuiltinInterceptor(typeResolver)
    private val enumGen = NativeEnumGenerator()
    private val typeAliasGen = TypeAliasGenerator(genericInterceptor)
    private val builtinClass = NativeBuiltinClassGenerator(
        typeResolver,
        builtinClassImplGen,
        defaultValue,
        methodGenerator,
        enumGen,
        genericInterceptor,
        typeAliasGen,
    )
    private val engineClassImplGen = EngineClassImplGen()
    private val engineMethodImplGen = EngineMethodImplGen(typeResolver)
    private val engineClass = NativeEngineClassGenerator(
        typeResolver,
        engineMethodImplGen,
        methodGenerator,
        enumGen,
        engineClassImplGen,
    )
    private val variantImplGen = VariantImplGen(typeResolver)
    private val variant = NativeVariantGenerator(typeResolver, enumGen, variantImplGen)
    private val nativeStructure = KNativeStructureGenerator(typeResolver, bodyGenerator)
    private val utilFuncImplGen = UtilityFunctionImplGen()
    private val utils = NativeUtilityFunctionGenerator(methodGenerator, utilFuncImplGen)

    context(context: Context)
    override fun generate(api: ExtensionApi): Sequence<FileSpec> = sequence {
        // Initialise impl generators with the resolved package registry.
        // This must happen inside generate() where Context is available.
        implPackageRegistry = ImplementationPackageRegistry(
            context.rootPackage,
            checkNotNull(context.extensionInterface) { "extensionInterface required for impl generation" },
        )
        builtinClassImplGen.initialize(implPackageRegistry)
        utilFuncImplGen.initialize(implPackageRegistry)
        variantImplGen.initialize(implPackageRegistry)
        nativeStructure.initialize(implPackageRegistry)
        engineClassImplGen.initialize(implPackageRegistry)
        engineMethodImplGen.initialize(implPackageRegistry)

        val builtinClassesPaths = context.model.builtins.asSequence().mapNotNull {
            builtinClass.generateFile(it)
        }

        yieldAll(builtinClassesPaths)

        yield(utils.generateFile(api.utilityFunctions))

        val nativeStructuresPaths = context.model.nativeStructures.asSequence().mapNotNull {
            nativeStructure.generateFile(it)
        }

        yieldAll(nativeStructuresPaths)

        val godotClassesPaths = context.model.engineClasses.asSequence().map {
            engineClass.generateFile(it)
        }

        yieldAll(godotClassesPaths)

        val (nestedEnums, globalEnums) = context.model.globalEnums.partition { it.ownerName != null }

        val globalEnumsPaths = globalEnums.asSequence().map {
            enumGen.generateFile(it)
        }
        yieldAll(globalEnumsPaths)

        if (nestedEnums.size > 2) {
            println(
                "WARNING: Nested enums (${nestedEnums.size}) [${nestedEnums.joinToString(postfix = "]") { it.name }}",
            )
        }

        // Builtin missing: Variant
        yield(variant.generateFile(nestedEnums))

        if (api.globalConstants.isNotEmpty()) {
            System.err.println(
                "WARNING: Global constants not supported yet. Found: [${api.globalConstants.joinToString()}]",
            )
        }
    }
}
