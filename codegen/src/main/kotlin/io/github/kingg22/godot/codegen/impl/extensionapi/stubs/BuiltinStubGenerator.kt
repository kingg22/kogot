package io.github.kingg22.godot.codegen.impl.extensionapi.stubs

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.godot.codegen.impl.commonConfiguration
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass

/**
 * Generates an `open class` stub for a Godot builtin class (Vector2, Color, etc.).
 */
class BuiltinStubGenerator(
    private val packageName: String,
    private val typeResolver: TypeResolver,
    private val enumGen: EnumStubGenerator,
) {
    private val methodGen = MethodStubGenerator(packageName, typeResolver)

    context(_: Context)
    fun generate(cls: BuiltinClass): FileSpec? {
        if (cls.name.lowercase() in MAPPED_GODOT_BUILTIN_CLASSES) return null
        val className = cls.name.renameGodotClass()

        return withExceptionContext({ "Generating builtin class '$className'" }) {
            val typeBuilder = TypeSpec.classBuilder(className)

            val companionBuilder = TypeSpec.companionObjectBuilder()

            val (staticMethods, instanceMethods) = cls.methods.partition { it.isStatic }

            fun buildMethod(method: BuiltinClass.BuiltinMethod) = withExceptionContext({
                "Error generating method '${method.name}', return: ${method.returnType}"
            }) {
                val returnType = method.returnType?.let { typeResolver.resolve(it) } ?: UNIT
                methodGen.generate(
                    name = method.name,
                    returnType = returnType,
                    returnTypeString = method.returnType,
                    isOpen = false,
                    arguments = method.arguments,
                )
            }

            for (staticMethod in staticMethods) {
                buildMethod(staticMethod).build().also {
                    companionBuilder.addFunction(it)
                }
            }

            for (instanceMethod in instanceMethods) {
                typeBuilder.addFunction(buildMethod(instanceMethod).build())
            }

            if (staticMethods.isNotEmpty()) {
                typeBuilder.addType(companionBuilder.build())
            }

            cls.enums.map { enumGen.generateSpec(it) }.forEach { typeBuilder.addType(it) }

            FileSpec
                .builder(packageName, className)
                .commonConfiguration()
                .addType(typeBuilder.build())
                .build()
        }
    }

    companion object {
        private val MAPPED_GODOT_BUILTIN_CLASSES = setOf("int", "float", "bool", "nil")
    }
}
