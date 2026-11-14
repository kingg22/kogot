package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedEngineClass

/**
 * Emits `<EngineClass>ClassNode`, one node of the engine class tree used to find the most derived wrapper of a
 * native object at runtime (issue #206).
 *
 * Every node only knows how to build its own class and which classes inherit from it **directly**, so no file
 * holds a map of all the classes. The `ClassNode` runtime interface walks them from `GodotObject` downwards.
 *
 * This is the only generated code that calls the wrapper's `(COpaquePointer)` constructor for a cast.
 *
 * @param classNodeName the runtime `ClassNode` interface, resolved lazily because the package registry is only
 * available once generation starts.
 */
class NativeEngineClassNodeGenerator(private val classNodeName: () -> ClassName) {

    context(context: Context)
    fun generateFile(cls: ResolvedEngineClass, children: List<ResolvedEngineClass>): FileSpec {
        val classNameStr = cls.name.renameGodotClass()
        val packageName = context.packageForOrDefault(cls.name)
        val objectName = nodeName(cls)
        val wrapperName = ClassName(packageName, classNameStr)
        val rootName = ClassName(context.packageForOrDefault(ROOT_CLASS), ROOT_CLASS.renameGodotClass())

        val classNode = classNodeName()
        val nodeType = classNode.parameterizedBy(rootName)
        val ptr = "ptr"
        val godotNameParam = "godotName"

        val create = if (cls.isSingleton) {
            // The constructor of a singleton is private or internal: its only instance is the companion's.
            CodeBlock.of("%T.instance", wrapperName)
        } else {
            CodeBlock.of("%T(%L)", wrapperName, ptr)
        }

        val typeSpec = TypeSpec
            .objectBuilder(objectName)
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(nodeType)
            .addProperty(
                PropertySpec
                    .builder(godotNameParam, STRING, KModifier.OVERRIDE)
                    .initializer("%S", cls.shortName)
                    .build(),
            )
            .addFunction(
                FunSpec
                    .builder("create")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(ptr, ClassName("kotlinx.cinterop", "COpaquePointer"))
                    .returns(rootName)
                    .addStatement("return %L", create)
                    .build(),
            )
            .addFunction(
                FunSpec
                    .builder("child")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(godotNameParam, STRING)
                    .returns(nodeType.copy(nullable = true))
                    .addCode(buildChildren(godotNameParam, children, context))
                    .build(),
            )
            .build()

        return createFile(typeSpec, objectName, packageName)
    }

    private fun buildChildren(
        godotNameParam: String,
        children: List<ResolvedEngineClass>,
        context: Context,
    ): CodeBlock = if (children.isEmpty()) {
        CodeBlock.of("return null\n")
    } else {
        CodeBlock
            .builder()
            .beginControlFlow("return when (%L)", godotNameParam)
            .apply {
                for (child in children.sortedBy { it.shortName }) {
                    addStatement(
                        "%S -> %T",
                        child.shortName,
                        ClassName(context.packageForOrDefault(child.name), nodeName(child)),
                    )
                }
            }
            .addStatement("else -> null")
            .endControlFlow()
            .build()
    }

    private fun nodeName(cls: ResolvedEngineClass) = "${cls.name.renameGodotClass()}ClassNode"

    companion object {
        /** The class every other engine class descends from, where the descent starts. */
        const val ROOT_CLASS = "Object"
    }
}
