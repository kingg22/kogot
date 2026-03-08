package io.github.kingg22.godot.codegen.impl.extensionapi.ffm

import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver

class JavaFfmTypeResolver : TypeResolver {
    context(context: Context)
    override fun resolve(godotType: String): TypeName = context.classNameForOrDefault(godotType)
}
