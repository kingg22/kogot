package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver.NativeStructureParser
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.impl.sanitizeTypeName
import io.github.kingg22.godot.codegen.models.extensionapi.NativeStructure

class KNativeStructureGenerator(private val typeResolver: TypeResolver, private val body: BodyGenerator) {

    context(context: Context)
    fun generateFile(ns: NativeStructure): FileSpec {
        val spec = generateSpec(ns)
        return createFile(spec, spec.name!!, context.packageForOrDefault(spec.name!!))
    }

    context(context: Context)
    fun generateSpec(ns: NativeStructure): TypeSpec {
        val typeBuilder = TypeSpec.classBuilder(sanitizeTypeName(ns.name.renameGodotClass()))

        typeBuilder.experimentalApiAnnotation(ns.name)

        NativeStructureParser
            .parseFormat(ns.format)
            .forEach { field ->
                val type = typeResolver.resolve(field.type)
                val resolvedType = if (field.arraySize != null) {
                    typeResolver.resolve("Array")
                } else {
                    type
                }

                PropertySpec
                    .builder(safeIdentifier(field.name), resolvedType)
                    .mutable(true)
                    .getter(body.todoGetter())
                    .setter(
                        FunSpec
                            .setterBuilder()
                            .addParameter("value", resolvedType)
                            .addCode(body.todoBody())
                            .build(),
                    )
                    .apply {
                        if (field.arraySize != null) {
                            addKdoc("Array size: %L, type: %T", field.arraySize, type)
                        }
                    }
                    .build()
                    .let { typeBuilder.addProperty(it) }
            }

        return typeBuilder.build()
    }
}
