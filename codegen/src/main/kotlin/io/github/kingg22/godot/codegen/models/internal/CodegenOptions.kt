package io.github.kingg22.godot.codegen.models.internal

data class CodegenOptions(
    val buildConfiguration: BuildConfiguration? = null,
    val pointerBits: Int = inferredPointerBits(),
) {
    fun resolveBuildConfiguration(precision: String): BuildConfiguration =
        buildConfiguration ?: BuildConfiguration.defaultFor(precision, pointerBits)

    companion object {
        private fun inferredPointerBits(): Int = when (System.getProperty("sun.arch.data.model")?.toIntOrNull()) {
            32 -> 32
            else -> 64
        }
    }
}
