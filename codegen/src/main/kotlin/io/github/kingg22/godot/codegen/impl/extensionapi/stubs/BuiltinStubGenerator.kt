package io.github.kingg22.godot.codegen.impl.extensionapi.stubs

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.godot.codegen.impl.commonConfiguration
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.jvmStaticAnnotation
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

    context(context: Context)
    fun generate(cls: BuiltinClass): FileSpec? {
        if (cls.name.lowercase() in MAPPED_GODOT_BUILTIN_CLASSES) return null
        val isSingleton = context.isSingleton(cls)
        val className = cls.name.renameGodotClass()

        return withExceptionContext({ "Generating builtin class '$className'" }) {
            val typeBuilder = if (isSingleton) {
                buildSingletonClass(className)
            } else {
                TypeSpec.Companion.classBuilder(className).addModifiers(KModifier.OPEN)
            }

            val companionBuilder = typeBuilder.typeSpecs
                .find { it.isCompanion }
                ?.also { typeBuilder.typeSpecs.remove(it) }
                ?.toBuilder()
                ?: TypeSpec.Companion.companionObjectBuilder()

            cls.methods.forEach { method ->
                withExceptionContext({ "Error generating method '${method.name}', return: ${method.returnType}" }) {
                    val returnType = method.returnType?.let { typeResolver.resolve(it) } ?: UNIT
                    val funSpec = methodGen.generate(
                        name = method.name,
                        returnType = returnType,
                        returnTypeString = method.returnType,
                        isOpen = !isSingleton && !method.isStatic,
                        arguments = method.arguments,
                    )
                    if (method.isStatic) {
                        companionBuilder.addFunction(funSpec.addAnnotation(jvmStaticAnnotation()).build())
                    } else {
                        typeBuilder.addFunction(funSpec.build())
                    }
                }
            }

            if (companionBuilder.build() != TypeSpec.Companion.companionObjectBuilder().build()) {
                typeBuilder.typeSpecs.addFirst(companionBuilder.build())
            }

            cls.enums.map { enumGen.generateSpec(it) }.forEach { typeBuilder.addType(it) }

            FileSpec.Companion.builder(packageName, className)
                .commonConfiguration()
                .addType(typeBuilder.build())
                .build()
        }
    }

    private fun buildSingletonClass(className: String): TypeSpec.Builder {
        val classType = ClassName(packageName, className)
        val lazyMethod = MemberName("kotlin", "lazy")
        val lazyMode = ClassName("kotlin", "LazyThreadSafetyMode")
        val companion = TypeSpec.Companion.companionObjectBuilder()
            .addProperty(
                PropertySpec.Companion.builder("instance", classType)
                    .delegate(
                        CodeBlock.Companion.builder()
                            .beginControlFlow("%M(%T.NONE)", lazyMethod, lazyMode)
                            .addStatement("%T()", classType)
                            .endControlFlow()
                            .build(),
                    )
                    .build(),
            )
            .build()
        return TypeSpec.Companion.classBuilder(classType)
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(FunSpec.Companion.constructorBuilder().addModifiers(KModifier.PROTECTED).build())
            .addType(companion)
    }

    companion object {
        private val MAPPED_GODOT_BUILTIN_CLASSES = setOf("int", "float", "bool", "nil")
    }
}
