package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtensionApi(
    val header: Header,
    @SerialName("builtin_class_sizes") val builtinClassSizes: List<BuiltinSizes>,
    @SerialName("builtin_class_member_offsets") val builtinClassMemberOffsets: List<BuiltinClassMemberOffsets>,
    @SerialName("builtin_classes") val builtinClasses: List<BuiltinClass>,
    val classes: List<GodotClass>,
    @SerialName("global_constants") val globalConstants: List<String>,
    @SerialName("global_enums") val globalEnums: List<ApiEnum>,
    @SerialName("utility_functions") val utilityFunctions: List<UtilityFunction>,
    @SerialName("native_structures") val nativeStructures: List<NativeStructure>,
    val singletons: List<Singleton>,
)
