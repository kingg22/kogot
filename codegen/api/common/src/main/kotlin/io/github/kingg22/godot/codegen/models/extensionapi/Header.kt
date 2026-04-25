package io.github.kingg22.godot.codegen.models.extensionapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Header(
    @SerialName("version_major") val versionMajor: Int,
    @SerialName("version_minor") val versionMinor: Int,
    @SerialName("version_patch") val versionPatch: Int,
    @SerialName("version_status") val versionStatus: String,
    @SerialName("version_build") val versionBuild: String,
    @SerialName("version_full_name") val versionFullName: String,
    val precision: String,
) {
    init {
        check(precision == "single" || precision == "double") {
            "Detected precision '$precision', but only single and double are supported."
        }
    }
}
