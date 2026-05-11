package io.github.kingg22.godot.api

@RequiresOptIn(
    message = "This is a Godot API that is not intended to be inherited from, " +
        "as the library may handle predefined instances of this in a special manner. " +
        "This will be an error in a future release. " +
        "If you need to inherit from this, please describe your use case in " +
        "https://github.com/kingg22/kogot/issues, so that we can provide a stable API for inheritance.",
    level = ERROR,
)
@Retention(BINARY)
@Target(CLASS)
@MustBeDocumented
public annotation class InternalForInheritanceGodotApi
