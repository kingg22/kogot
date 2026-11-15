package io.github.kingg22.kogot.processor.generators.kotlin

import io.github.kingg22.kogot.processor.model.AnnotationInfo
import io.github.kingg22.kogot.processor.model.ClassInfo
import io.github.kingg22.kogot.processor.model.FunctionInfo
import io.github.kingg22.kogot.processor.model.TypeInfo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A `@Godot` class whose parent is another `@Godot` class (issue #212): Godot must see the user parent in the class
 * hierarchy, but the native object must be built as the nearest engine class. Building the user parent runs its own
 * `create_instance`, which gives the object a second Kotlin instance and takes its instance-binding slot.
 */
class GodotBindingGeneratorInheritanceTest {
    private val generator = GodotBindingGenerator()

    private val godot =
        AnnotationInfo(qualifiedName = "io.github.kingg22.godot.api.annotations.Godot", shortName = "Godot")
    private val exportMethod = AnnotationInfo(
        qualifiedName = "io.github.kingg22.godot.api.annotations.ExportMethod",
        shortName = "ExportMethod",
    )

    private fun godotClass(
        name: String,
        parent: TypeInfo,
        engineBase: String? = null,
        functions: List<FunctionInfo> = emptyList(),
    ) = ClassInfo(
        qualifiedName = "game.$name",
        shortName = name,
        packageName = "game",
        supertypes = listOf(parent),
        annotations = listOf(godot),
        functions = functions,
        filePath = "/project/src/$name.kt",
        engineBaseClassName = engineBase,
    )

    private fun binding(classInfo: ClassInfo): String = generator.generate(listOf(classInfo)).files.single {
        it.relativePath.endsWith("${classInfo.shortName}_Binding.kt")
    }
        .content

    @Test
    fun `a class extending a @Godot class registers that parent but builds the engine base`() {
        val derived = godotClass("Derived", TypeInfo("game.Base", "Base"), engineBase = "Node")

        val content = binding(derived)

        assertTrue(content.contains("\"Derived\",\n            \"Base\","), content)
        assertTrue(content.contains("createInstanceFunc(\"Node\", \"Derived\""), content)
        assertFalse(content.contains("createInstanceFunc(\"Base\""), content)
    }

    @Test
    fun `a class extending an engine class builds that class`() {
        val content =
            binding(godotClass("Player", TypeInfo("io.github.kingg22.godot.api.core.node.Sprite2D", "Sprite2D")))

        assertTrue(content.contains("\"Player\",\n            \"Sprite2D\","), content)
        assertTrue(content.contains("createInstanceFunc(\"Sprite2D\", \"Player\""), content)
    }

    @Test
    fun `a method without parameters ignores args and rejects arguments`() {
        val greeting = FunctionInfo(
            name = "greeting",
            returnType = TypeInfo("kotlin.Int", "Int"),
            annotations = listOf(exportMethod),
        )
        val content =
            binding(
                godotClass(
                    "Greeter",
                    TypeInfo("io.github.kingg22.godot.api.core.Node", "Node"),
                    functions = listOf(greeting),
                ),
            )

        assertTrue(content.contains("staticCFunction { _, instancePtr, _, argCount, returnValue, rError ->"), content)
        assertTrue(content.contains("if (argCount != 0L)"), content)
        assertTrue(content.contains("GDEXTENSION_CALL_ERROR_TOO_MANY_ARGUMENTS, argCount.toInt(), 0"), content)
    }
}
