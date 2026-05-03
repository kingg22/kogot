package io.github.kingg22.godot.codegen.callable.knative

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.services.PackageRegistry
import io.github.kingg22.godot.codegen.types.K_CHECK
import io.github.kingg22.godot.codegen.types.K_ERROR
import io.github.kingg22.godot.codegen.types.K_SUPPRESS

private const val MIN_FUNCTION_ARITY = 0
private const val MAX_FUNCTION_ARITY = 23
private val K_FUNCTION = ClassName("kotlin", "Function")
private val K_FUNCTIONS = buildMap {
    (MIN_FUNCTION_ARITY..MAX_FUNCTION_ARITY).forEach { i ->
        put(i, ClassName("kotlin", "Function$i"))
    }
}
private val ANY_NULLABLE = ANY.copy(nullable = true)

class KotlinNativeCallableGenerator(private val packageName: String, private val packageRegistry: PackageRegistry) {

    fun generate(): Sequence<FileSpec> = sequence {
        yield(generateCallables())
        yield(generateHelpers())
    }

    private fun generateCallables(): FileSpec = createFile(
        "Callables",
        packageRegistry.packageForOrDefault("KotlinCallable"),
    ) {
        (MIN_FUNCTION_ARITY..MAX_FUNCTION_ARITY).forEach { arity ->
            val kotlinFunction = K_FUNCTIONS.getValue(arity).parameterizedBy(
                List(arity + 1) { ANY_NULLABLE },
            )

            val callableTypeSpec = TypeSpec
                .classBuilder("Callable$arity")
                .addModifiers(KModifier.VALUE)
                .addAnnotation(packageRegistry.classNameForOrDefault("InternalBinding"))
                .addSuperinterface(packageRegistry.classNameForOrDefault("KotlinCallable"))
                .addSuperinterface(kotlinFunction, delegate = CodeBlock.of("lambda"))
                .addProperty(PropertySpec.builder("lambda", kotlinFunction).initializer("lambda").build())
                .primaryConstructor(
                    FunSpec
                        .constructorBuilder()
                        .addParameter("lambda", kotlinFunction)
                        .build(),
                )
                .addFunction(
                    FunSpec
                        .builder("arity")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(LONG)
                        .addCode("return %L", arity)
                        .build(),
                )
                .addFunction(
                    FunSpec
                        .builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(STRING)
                        .addCode($$"return \"Callable%L(lambda=$lambda)\"", arity)
                        .build(),
                )
                .build()

            addType(callableTypeSpec)
        }
    }

    private fun generateHelpers(): FileSpec = createFile("Helpers", packageName) {
        addFunction(
            FunSpec
                .builder("invoke")
                .apply {
                    addModifiers(KModifier.OPERATOR)
                    receiver(packageRegistry.classNameForOrDefault("KotlinCallable"))
                    addAnnotation(packageRegistry.classNameForOrDefault("InternalBinding"))
                    val argsParameter = "args"

                    addParameter(argsParameter, ARRAY.parameterizedBy(STAR))
                    returns(ANY_NULLABLE)

                    addKdoc("Invokes the callable with the given arguments\n\n")
                    addKdoc("**SAFETY:** Assumes the arguments are compatible with the callable's arity.")

                    addCode(
                        CodeBlock
                            .builder()
                            .addStatement("%M(%L.size.toLong() == this.arity())", K_CHECK, argsParameter)
                            .beginControlFlow("return when (this)")
                            .apply {
                                (MIN_FUNCTION_ARITY..MAX_FUNCTION_ARITY).forEach { arity ->
                                    addStatement(
                                        "is %T -> this(%L)",
                                        packageRegistry.classNameForOrDefault("Callable$arity"),
                                        if (arity != 0) {
                                            (MIN_FUNCTION_ARITY until arity).joinToString(",♢") { arg -> "args[$arg]" }
                                        } else {
                                            ""
                                        },
                                    )
                                }
                            }.endControlFlow()
                            .build(),
                    )
                }.build(),
        )

        addFunction(
            FunSpec
                .builder("wrapLambda")
                .apply {
                    addAnnotation(packageRegistry.classNameForOrDefault("InternalBinding"))
                    addAnnotation(
                        AnnotationSpec
                            .builder(K_SUPPRESS)
                            .addMember("%S", "UNCHECKED_CAST")
                            .addMember("%S", "USELESS_CAST")
                            .build(),
                    )
                    addParameter("lambda", K_FUNCTION.parameterizedBy(STAR))
                    returns(packageRegistry.classNameForOrDefault("KotlinCallable"))
                    beginControlFlow("return when (lambda)")
                        .apply {
                            (MIN_FUNCTION_ARITY..MAX_FUNCTION_ARITY).forEach { arity ->
                                val kotlinFunction = K_FUNCTIONS.getValue(arity)
                                addStatement(
                                    "is %T ->♢%T(lambda as %T)",
                                    kotlinFunction.parameterizedBy(
                                        List(arity + 1) { STAR },
                                    ),
                                    packageRegistry.classNameForOrDefault("Callable$arity"),
                                    kotlinFunction.parameterizedBy(
                                        List(arity) { ANY_NULLABLE } + STAR,
                                    ),
                                )
                            }
                            addStatement(
                                $$"else -> %M(\"Unsupported lambda type: ${lambda::class}, too many arguments\")",
                                K_ERROR,
                            )
                        }
                    endControlFlow()
                }.build(),
        )
    }
}
