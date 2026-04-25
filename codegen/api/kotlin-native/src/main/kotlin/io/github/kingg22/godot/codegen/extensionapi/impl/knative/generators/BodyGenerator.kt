package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import io.github.kingg22.godot.codegen.types.K_TODO

/** Responsible solely for generating function/property bodies. */
object BodyGenerator {
    fun todoBody(message: String? = null): CodeBlock = if (message != null) {
        CodeBlock.of("%M(%S)", K_TODO, message)
    } else {
        CodeBlock.of("%M()", K_TODO)
    }

    fun todoGetter(message: String? = null): FunSpec = FunSpec
        .getterBuilder()
        .addCode(todoBody(message))
        .build()
}
