// RUN_PIPELINE_TILL: FRONTEND
// Black box: @MustBeVariant targets TYPE and TYPE_PARAMETER only, so declarations and expressions are rejected by
// Kotlin itself. On any type, it is a contract checked wherever a value enters it (see typedContracts.kt).

import io.github.kingg22.godot.api.builtin.*

<!WRONG_ANNOTATION_TARGET!>@MustBeVariant<!> class OnClass
<!WRONG_ANNOTATION_TARGET!>@MustBeVariant<!> fun onFunction() {}
fun onParameterDeclaration(<!WRONG_ANNOTATION_TARGET!>@MustBeVariant<!> value: Any?) {}

fun inBody() {
    <!WRONG_ANNOTATION_TARGET!>@MustBeVariant<!> val onLocalDeclaration = 1
    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!><!WRONG_ANNOTATION_TARGET!>@MustBeVariant<!> 1 + 1<!>
}

// Type positions are allowed and checked.
fun <@MustBeVariant T> onTypeParameter(value: T) {}
fun onParameterType(value: @MustBeVariant Any?) {}
fun onReturnType(): @MustBeVariant Any? = 1
val onPropertyType: @MustBeVariant Any? = 1

fun onLocalType() {
    val local: @MustBeVariant Any? = 1
}

// Only function types and Godot typed containers give a type argument a meaning.
fun onTypeArguments(
    action: () -> @MustBeVariant Any?,
    array: GodotArray<@MustBeVariant Any?>,
    list: <!MUST_BE_VARIANT_NO_EFFECT!>List<@MustBeVariant Any?><!>,
    map: <!MUST_BE_VARIANT_NO_EFFECT!>Map<String, @MustBeVariant Any?><!>,
) {}
