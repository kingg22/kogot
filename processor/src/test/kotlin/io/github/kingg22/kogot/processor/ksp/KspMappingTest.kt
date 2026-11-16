package io.github.kingg22.kogot.processor.ksp

import org.junit.jupiter.api.Test

/** The KSP mapping and validation on real Kotlin sources, not hand-built `ClassInfo`s. */
class KspMappingTest {
    @Test
    fun `a godot class generates its binding`() = runKogotProcessor(
        """
        package game

        import io.github.kingg22.godot.api.annotations.Godot
        import io.github.kingg22.godot.api.core.Node

        @Godot
        class Player : Node()
        """,
    ) {
        hasNoKogotDiagnostics()
        generatedSourceFileWithPath("game/Player_Binding.kt").contains("Player_Binding")
    }

    @Test
    fun `typed signal parameters are inferred through aliases and keep their names`() = runKogotProcessor(
        """
        package game

        import io.github.kingg22.godot.api.annotations.Godot
        import io.github.kingg22.godot.api.annotations.RegisterSignal
        import io.github.kingg22.godot.api.core.Node
        import io.github.kingg22.godot.api.signal.signal

        typealias Hit = (damage: Long, by: Node?) -> Unit

        @Godot
        class Player : Node() {
            @RegisterSignal val hit by signal<Hit>()
        }
        """,
    ) {
        hasNoKogotDiagnostics()
        generatedSourceFileWithPath("game/Player_Binding.kt").apply {
            contains("\"damage\"")
            contains("\"by\"")
        }
    }

    @Test
    fun `an unnamed signal parameter is reported on the real declaration`() = runKogotProcessor(
        """
        package game

        import io.github.kingg22.godot.api.annotations.Godot
        import io.github.kingg22.godot.api.annotations.RegisterSignal
        import io.github.kingg22.godot.api.core.Node
        import io.github.kingg22.godot.api.signal.signal

        @Godot
        class Player : Node() {
            @RegisterSignal val hit by signal<(Long) -> Unit>()
        }
        """,
    ) {
        hasErrorContaining("KOGOT-304")
    }

    @Test
    fun `a Byte signal parameter is rejected`() = runKogotProcessor(
        """
        package game

        import io.github.kingg22.godot.api.annotations.Godot
        import io.github.kingg22.godot.api.annotations.RegisterSignal
        import io.github.kingg22.godot.api.core.Node
        import io.github.kingg22.godot.api.signal.signal

        @Godot
        class Player : Node() {
            @RegisterSignal val small by signal<(value: Byte) -> Unit>()
        }
        """,
    ) {
        hasErrorContaining("KOGOT-305")
    }

    @Test
    fun `a signal object parameter is registered under the nearest registered class`() = runKogotProcessor(
        """
        package game

        import io.github.kingg22.godot.api.annotations.Godot
        import io.github.kingg22.godot.api.annotations.RegisterSignal
        import io.github.kingg22.godot.api.core.Node
        import io.github.kingg22.godot.api.internal.GodotClass
        import io.github.kingg22.godot.api.signal.signal

        @GodotClass("Sprite")
        open class EngineSprite : Node()

        @Godot
        class Enemy : Node()

        // Not registered: Godot sees it as its closest registered ancestor.
        class Boss : EngineSprite()

        @Godot
        class Player : Node() {
            @RegisterSignal val engine by signal<(sprite: EngineSprite) -> Unit>()
            @RegisterSignal val user by signal<(enemy: Enemy) -> Unit>()
            @RegisterSignal val unregistered by signal<(boss: Boss) -> Unit>()
        }
        """,
    ) {
        hasNoKogotDiagnostics()
        generatedSourceFileWithPath("game/Player_Binding.kt").apply {
            contains("\"Sprite\"")
            contains("\"Enemy\"")
        }
    }
}
