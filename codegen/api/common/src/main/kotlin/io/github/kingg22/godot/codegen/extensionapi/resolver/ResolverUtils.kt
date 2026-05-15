package io.github.kingg22.godot.codegen.extensionapi.resolver

import com.squareup.kotlinpoet.Annotatable
import com.squareup.kotlinpoet.AnnotationSpec
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.models.extensionapi.Documentable
import com.squareup.kotlinpoet.Documentable as KPoetDocumentable

@IgnorableReturnValue
context(ctx: Context)
fun <T : Annotatable.Builder<T>> T.experimentalApiAnnotation(className: String, memberName: String? = null): T {
    if (ctx.isExperimentalType(className, memberName)) {
        addAnnotation(
            AnnotationSpec
                .builder(ctx.classNameOfExperimentalAnnotation())
                .apply {
                    val reason = ctx.getReasonOfExperimental(className, memberName)
                    if (!reason.isNullOrBlank()) addMember("reason = %S", reason)
                }.build(),
        )
    }
    return this
}

@IgnorableReturnValue
context(_: Context)
fun <T : KPoetDocumentable.Builder<T>> T.addKdocIfPresent(documentable: Documentable): T {
    if (documentable.description.isNullOrBlank()) return this
    val formattedDoc = KDocFormatter.format(documentable.description!!)
    addKdoc("%L", formattedDoc)
    return this
}
