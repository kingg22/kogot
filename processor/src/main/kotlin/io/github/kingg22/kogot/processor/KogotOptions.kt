package io.github.kingg22.kogot.processor

/**
 * Output mode for the processor.
 */
enum class OutputMode {
    /** Generate Kotlin binding code only (.kt files) */
    KOTLIN,

    /** Generate JSON manifest only */
    JSON,

    /** Generate both Kotlin and JSON outputs */
    BOTH,
}

/**
 * Configuration options for the Kogot processor.
 */
data class KogotOptions(
    val outputMode: OutputMode = OutputMode.BOTH,
    val generatedPackage: String = "io.github.kingg22.godot.generated",
    val logLevel: LogLevel = LogLevel.WARN,
) {
    enum class LogLevel { DEBUG, INFO, WARN, ERROR }

    companion object {
        const val OUTPUT_MODE_KEY = "kogot.outputMode"
        const val GENERATED_PACKAGE_KEY = "kogot.generatedPackage"
        const val LOG_LEVEL_KEY = "kogot.logLevel"

        /**
         * Parses options from KSP processor options map.
         */
        fun fromMap(options: Map<String, String>): KogotOptions = KogotOptions(
            outputMode = options[OUTPUT_MODE_KEY]?.let {
                OutputMode.valueOf(it.uppercase())
            } ?: OutputMode.BOTH,
            generatedPackage = options[GENERATED_PACKAGE_KEY] ?: "io.github.kingg22.godot.generated",
            logLevel = options[LOG_LEVEL_KEY]?.let {
                LogLevel.valueOf(it.uppercase())
            } ?: LogLevel.WARN,
        )
    }
}
