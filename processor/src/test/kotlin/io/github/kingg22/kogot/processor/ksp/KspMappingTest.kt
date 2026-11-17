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
        generatedSourceFileWithPath("game/Player_Binding.kt").apply {
            // Real binding code, not just a comment that mentions the name.
            contains("public object Player_Binding")
            contains("public fun register()")
            contains("registerClass<Player>(")
            contains("public fun unregister()")
        }
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
            // Each parameter keeps its own name and gets the className of its nearest registered class.
            contains("""RegisterSignal.Param(Variant.Type.OBJECT, "sprite", className = "Sprite")""")
            contains("""RegisterSignal.Param(Variant.Type.OBJECT, "enemy", className = "Enemy")""")
            // Boss is not registered: it is exposed as its closest registered ancestor, not under its own name.
            contains("""RegisterSignal.Param(Variant.Type.OBJECT, "boss", className = "Sprite")""")
            doesNotContain("\"Boss\"")
        }
    }

    @Test
    fun `a signal object parameter from a compiled dependency is registered under its nearest registered class`() =
        runKogotProcessorWithDependency(
            dependency = """
            package lib

            import io.github.kingg22.godot.api.annotations.Godot
            import io.github.kingg22.godot.api.core.Node
            import io.github.kingg22.godot.api.internal.GodotClass

            @GodotClass("Sprite")
            open class LibSprite : Node()

            @Godot
            class LibEnemy : Node()

            // Not registered: its closest registered ancestor is the engine class.
            open class LibBoss : LibSprite()
            """,
            """
            package game

            import io.github.kingg22.godot.api.annotations.Godot
            import io.github.kingg22.godot.api.annotations.RegisterSignal
            import io.github.kingg22.godot.api.core.Node
            import io.github.kingg22.godot.api.signal.signal
            import lib.LibBoss
            import lib.LibEnemy
            import lib.LibSprite

            @Godot
            class Player : Node() {
                @RegisterSignal val engine by signal<(sprite: LibSprite) -> Unit>()
                @RegisterSignal val user by signal<(enemy: LibEnemy) -> Unit>()
                @RegisterSignal val unregistered by signal<(boss: LibBoss) -> Unit>()
            }
            """,
        ) {
            hasNoKogotDiagnostics()
            generatedSourceFileWithPath("game/Player_Binding.kt").apply {
                contains("""RegisterSignal.Param(Variant.Type.OBJECT, "sprite", className = "Sprite")""")
                contains("""RegisterSignal.Param(Variant.Type.OBJECT, "enemy", className = "LibEnemy")""")
                contains("""RegisterSignal.Param(Variant.Type.OBJECT, "boss", className = "Sprite")""")
            }
        }
}
