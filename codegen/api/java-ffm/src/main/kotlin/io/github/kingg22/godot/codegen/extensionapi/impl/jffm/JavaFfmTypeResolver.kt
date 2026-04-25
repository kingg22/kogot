package io.github.kingg22.godot.codegen.extensionapi.impl.jffm

import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver

class JavaFfmTypeResolver : TypeResolver {
    context(ctx: Context)
    override fun resolve(godotType: String, metaType: String?): TypeName = ctx.classNameForOrDefault(godotType)
}
