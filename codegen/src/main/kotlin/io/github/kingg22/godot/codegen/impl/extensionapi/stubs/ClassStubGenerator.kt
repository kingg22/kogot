package io.github.kingg22.godot.codegen.impl.extensionapi.stubs

import com.squareup.kotlinpoet.*
import io.github.kingg22.godot.codegen.impl.commonConfiguration
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.jvmNameAnnotation
import io.github.kingg22.godot.codegen.impl.jvmStaticAnnotation
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass

/**
 * Generates an `open class` or singleton `object`-style class for a Godot engine class.
 */
class ClassStubGenerator(private val packageName: String, private val typeResolver: TypeResolver) {
    private val methodGen = MethodStubGenerator(typeResolver)

    context(context: Context)
    fun generate(cls: EngineClass): FileSpec {
        val isSingleton = context.isSingleton(cls)
        val className = cls.name.renameGodotClass()

        return withExceptionContext({ "Generating class '${cls.name}', isSingleton: $isSingleton" }) {
            val parentTypeName = cls.inherits
                ?.takeIf { it.isNotBlank() }
                ?.let { typeResolver.resolve(it) }

            val typeBuilder = if (isSingleton) {
                buildSingletonClass(className, parentTypeName)
            } else {
                buildRegularClass(className, parentTypeName)
            }

            val companionBuilder = typeBuilder.typeSpecs
                .find { it.isCompanion }
                ?.also { typeBuilder.typeSpecs.remove(it) }
                ?.toBuilder()
                ?: TypeSpec.Companion.companionObjectBuilder()

            cls.methods.forEach { method ->
                withExceptionContext({ "Error generating method '${method.name}'" }) {
                    val funSpec = methodGen.generate(
                        method.name,
                        method.returnValue?.let { typeResolver.resolve(it) } ?: UNIT,
                        method.returnValue?.type,
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

            cls.enums.map { EnumStubGenerator(packageName).generateSpec(it) }
                .forEach { typeBuilder.addType(it) }

            FileSpec.Companion.builder(packageName, className)
                .commonConfiguration()
                .addType(typeBuilder.build())
                .build()
        }
    }

    private fun buildRegularClass(className: String, baseClass: TypeName?): TypeSpec.Builder =
        TypeSpec.Companion.classBuilder(className)
            .addModifiers(KModifier.OPEN)
            .apply { baseClass?.let { superclass(it) } }

    private fun buildSingletonClass(className: String, baseClass: TypeName?): TypeSpec.Builder {
        val classType = ClassName(packageName, className)
        val lazyMethod = MemberName("kotlin", "lazy")
        val lazyMode = ClassName("kotlin", "LazyThreadSafetyMode")

        val companion = TypeSpec.Companion.companionObjectBuilder()
            .addProperty(
                PropertySpec.Companion.builder("instance", classType)
                    .addAnnotation(
                        jvmNameAnnotation("instance")
                            .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                            .build(),
                    )
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
            .apply { baseClass?.let { superclass(it) } }
    }
}
