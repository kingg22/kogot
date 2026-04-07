package io.github.kingg22.godot.api.annotations

/**
 * Add a custom icon to the current script.
 *
 * The icon specified at iconPath is displayed in the Scene dock for every node of that
 * class, as well as in various editor dialogs.
 *
 * Note: Only the script can have a custom icon. Inner classes are not supported.
 *
 * Note: As annotations describe their subject, the @icon annotation must be placed before
 * the class definition and inheritance.
 *
 * Note: Unlike most other annotations, the argument of the @icon annotation must be a
 * string literal (constant expressions are not supported).
 *
 * @param iconPath The path to the icon file (e.g., "res://path/to/class/icon.svg").
 */
@Retention(SOURCE)
@Target(CLASS)
@MustBeDocumented
public annotation class Icon(public val iconPath: String)

/**
 * Make a script with static variables to not persist after all references are lost.
 *
 * If the script is loaded again the static variables will revert to their default values.
 *
 * Note: As annotations describe their subject, the @static_unload annotation must be
 * placed before the class definition and inheritance.
 *
 * Warning: Currently, due to a bug, scripts are never freed, even if @static_unload
 * annotation is used.
 */
@Retention(SOURCE)
@Target(CLASS)
@MustBeDocumented
public annotation class StaticUnload

/**
 * Mark the current script as a tool script, allowing it to be loaded and executed by
 * the editor.
 *
 * Note: As annotations describe their subject, the @tool annotation must be placed before
 * the class definition and inheritance.
 */
@Retention(SOURCE)
@Target(CLASS)
@MustBeDocumented
public annotation class Tool
