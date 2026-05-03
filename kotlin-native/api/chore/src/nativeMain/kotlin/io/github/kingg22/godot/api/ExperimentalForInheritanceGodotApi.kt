package io.github.kingg22.godot.api

/** Marks declarations that cannot be safely inherited from. */
@RequiresOptIn(
    message = "Inheriting from this Godot Kotlin API is unstable. " +
        "Either new methods may be added in the future, which would break the inheritance, " +
        "or correctly inheriting from it requires fulfilling contracts that may change in the future.",
    level = ERROR,
)
@Retention(BINARY)
@Target(CLASS)
public annotation class ExperimentalForInheritanceGodotApi
