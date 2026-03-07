package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import io.github.kingg22.godot.codegen.impl.K_TODO

/** Responsible solely for generating function/property bodies. */
class BodyGenerator {
    fun todoBody(): CodeBlock = CodeBlock.of("%M()", K_TODO)

    fun todoGetter(): FunSpec = FunSpec
        .getterBuilder()
        .addCode(todoBody())
        .build()
}
