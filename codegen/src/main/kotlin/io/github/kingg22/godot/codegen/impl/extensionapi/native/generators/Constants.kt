package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.Annotatable
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver.KDocFormatter
import io.github.kingg22.godot.codegen.models.extensionapi.Documentable
import com.squareup.kotlinpoet.Documentable as KDocumentable

val API_STATUS_NON_EXTENSIBLE = ClassName("org.jetbrains.annotations", "ApiStatus", "NonExtendable")

context(context: Context)
fun <T : Annotatable.Builder<T>> T.experimentalApiAnnotation(className: String, memberName: String? = null): T {
    val isExperimental = context.isExperimentalType(className, memberName)
    if (isExperimental) {
        addAnnotation(
            AnnotationSpec
                .builder(context.classNameOfExperimentalAnnotation())
                .apply {
                    val reason = context.getReasonOfExperimental(className, memberName)
                    if (!reason.isNullOrBlank()) addMember("reason = %S", reason)
                }.build(),
        )
    }
    return this
}

context(_: Context)
fun <T : KDocumentable.Builder<T>> T.addKdocIfPresent(documentable: Documentable): T {
    if (documentable.description.isNullOrBlank()) return this
    val formattedDoc = KDocFormatter.format(documentable.description!!)!!
    addKdoc("%L", formattedDoc)
    return this
}
