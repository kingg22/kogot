package io.github.kingg22.kogot.processor.validation

/**
 * Orchestrates execution of multiple validators in sequence.
 */
class ValidatorPipeline(private val validators: List<Validator>) {
    /**
     * Executes all validators in order, aggregating results.
     *
     * @param context The validation context
     * @param stopOnFirstError If true, stops at first fatal error
     * @return Combined ValidationResult
     */
    fun execute(context: ValidationContext, stopOnFirstError: Boolean = true): ValidationResult {
        val combinedResult = ValidationResultImpl()

        for (validator in validators) {
            val result = validator.validate(context)
            combinedResult.merge(result)

            if (stopOnFirstError && !result.isValid) {
                break
            }
        }

        return combinedResult
    }

    /**
     * Creates a new pipeline with an additional validator.
     */
    fun with(validator: Validator): ValidatorPipeline = ValidatorPipeline(validators + validator)

    companion object {
        /**
         * Creates an empty pipeline.
         */
        fun empty(): ValidatorPipeline = ValidatorPipeline(emptyList())
    }
}
