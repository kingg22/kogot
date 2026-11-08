package io.github.kingg22.kogot.compiler.test

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

/** Generates one JUnit 5 test per file in `testData/`. Args: output directory, test data root. */
fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testsRoot = args[0], testDataRoot = args[1]) {
            testClass<AbstractKogotDiagnosticTest> {
                model("diagnostics")
            }
        }
    }
}
