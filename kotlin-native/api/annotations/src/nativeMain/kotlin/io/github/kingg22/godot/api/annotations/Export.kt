package io.github.kingg22.godot.api.annotations

import io.github.kingg22.godot.api.global.PropertyHint
import io.github.kingg22.godot.api.global.PropertyUsageFlags

/**
 * Mark the following property as exported (editable in the Inspector dock and saved to disk).
 *
 * Custom resources and nodes should be registered as global classes using `class_name`,
 * since the Inspector currently only supports global classes. Otherwise, a less specific
 * type will be exported instead.
 *
 * Node export is only supported in [io.github.kingg22.godot.api.core.Node]-derived classes
 * and has a number of other limitations.
 *
 * @see ExportCategory
 * @see ExportGroup
 * @see ExportSubgroup
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class Export

/**
 * Define a new category for the following exported properties.
 *
 * This helps to organize properties in the Inspector dock.
 *
 * Note: Categories in the Inspector dock's list usually divide properties coming from
 * different classes. For better clarity, it's recommended to use [ExportGroup] and
 * [ExportSubgroup] instead.
 *
 * @param name The name of the category displayed in the Inspector.
 * @see Export
 * @see ExportGroup
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportCategory(public val name: String)

/**
 * Export a [io.github.kingg22.godot.api.builtin.Color], [io.github.kingg22.godot.api.builtin.GodotArray]
 * of Color, or [io.github.kingg22.godot.api.builtin.PackedColorArray] property without allowing
 * its transparency (Color.a) to be edited.
 *
 * @see PropertyHint.COLOR_NO_ALPHA
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportColorNoAlpha

/**
 * Allows you to set a custom hint, hint string, and usage flags for the exported property.
 *
 * Note that there's no validation done in GDScript; it will just pass the parameters to the editor.
 *
 * Note: Regardless of the usage value, the PROPERTY_USAGE_SCRIPT_VARIABLE flag is always added,
 * as with any explicitly declared script variable.
 *
 * @param hint The [PropertyHint] for the editor.
 * @param hintString Additional hint string for the editor.
 * @param usage The bitfield of [PropertyUsageFlags] for the property. Defined as `vararg` due limitations.
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportCustom(
    public val hint: PropertyHint,
    public val hintString: String = "",
    public vararg val usage: PropertyUsageFlags = [DEFAULT],
)

/**
 * Export a [String], [io.github.kingg22.godot.api.builtin.GodotArray] of String,
 * or [io.github.kingg22.godot.api.builtin.PackedStringArray] property as a path to a directory.
 *
 * The path will be limited to the project folder and its subfolders.
 * See [ExportGlobalDir] to allow picking from the entire filesystem.
 *
 * @see PropertyHint.DIR
 * @see ExportGlobalDir
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportDir

/**
 * Export an [Int], [String], [io.github.kingg22.godot.api.builtin.GodotArray] of Int,
 * [io.github.kingg22.godot.api.builtin.GodotArray] of String,
 * [io.github.kingg22.godot.api.builtin.PackedByteArray],
 * [io.github.kingg22.godot.api.builtin.PackedInt32Array],
 * [io.github.kingg22.godot.api.builtin.PackedInt64Array], or
 * [io.github.kingg22.godot.api.builtin.PackedStringArray] property as an enumerated list
 * of options (or an array of options).
 *
 * If the property is an Int, then the index of the value is stored; in the same order the
 * values are provided. You can add explicit values using a colon. If the property is a
 * String, then the value is stored.
 *
 * If you want to use named enums, then use [Export] instead.
 *
 * @param names The names of the enumeration options.
 * @see PropertyHint.ENUM
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportEnum(public vararg val names: String)

/**
 * Export a floating-point property with an easing editor widget.
 *
 * Additional hints can be provided to adjust the behavior of the widget.
 * `attenuation` flips the curve, which makes it more intuitive for editing attenuation
 * properties. `positive_only` limits values to only be greater than or equal to zero.
 *
 * @param hints Additional hints for the easing editor ("attenuation", "positive_only").
 * @see PropertyHint.EXP_EASING
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportExpEasing(public vararg val hints: String)

/**
 * Export a [String], [io.github.kingg22.godot.api.builtin.GodotArray] of String,
 * or [io.github.kingg22.godot.api.builtin.PackedStringArray] property as a path to a file.
 *
 * The path will be limited to the project folder and its subfolders.
 * See [ExportGlobalFile] to allow picking from the entire filesystem.
 *
 * If a filter is provided, only matching files will be available for picking.
 *
 * Note: The file will be stored and referenced as UID, if available. This ensures that
 * the reference is valid even when the file is moved.
 *
 * @param filter File filter patterns (e.g., "*.txt").
 * @see PropertyHint.FILE
 * @see ExportGlobalFile
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportFile(public vararg val filter: String)

/**
 * Same as [ExportFile], except the file will be stored as a raw path.
 *
 * This means that it may become invalid when the file is moved. If you are exporting a
 * Resource path, consider using [ExportFile] instead.
 *
 * @param filter File filter patterns (e.g., "*.txt").
 * @see PropertyHint.FILE_PATH
 * @see ExportFile
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportFilePath(public vararg val filter: String)

/**
 * Export an integer property as a bit flag field.
 *
 * This allows you to store several "checked" or true values with one property and comfortably
 * select them from the Inspector dock.
 *
 * You can add explicit values using a colon. You can also combine several flags.
 *
 * Note: A flag value must be at least 1 and at most 2^32 - 1.
 *
 * @param names The names of the flag options, with optional explicit values (e.g., "Fire", "Water:2").
 * @see PropertyHint.FLAGS
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportFlags(public vararg val names: String)

/**
 * Export an integer property as a bit flag field for 2D navigation layers.
 *
 * The widget in the Inspector dock will use the layer names defined in
 * ProjectSettings.layer_names/2d_navigation/layer_1.
 *
 * @see PropertyHint.LAYERS_2D_NAVIGATION
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportFlags2dNavigation

/**
 * Export an integer property as a bit flag field for 2D physics layers.
 *
 * The widget in the Inspector dock will use the layer names defined in
 * ProjectSettings.layer_names/2d_physics/layer_1.
 *
 * @see PropertyHint.LAYERS_2D_PHYSICS
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportFlags2dPhysics

/**
 * Export an integer property as a bit flag field for 2D render layers.
 *
 * The widget in the Inspector dock will use the layer names defined in
 * ProjectSettings.layer_names/2d_render/layer_1.
 *
 * @see PropertyHint.LAYERS_2D_RENDER
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportFlags2dRender

/**
 * Export an integer property as a bit flag field for 3D navigation layers.
 *
 * The widget in the Inspector dock will use the layer names defined in
 * ProjectSettings.layer_names/3d_navigation/layer_1.
 *
 * @see PropertyHint.LAYERS_3D_NAVIGATION
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportFlags3dNavigation

/**
 * Export an integer property as a bit flag field for 3D physics layers.
 *
 * The widget in the Inspector dock will use the layer names defined in
 * ProjectSettings.layer_names/3d_physics/layer_1.
 *
 * @see PropertyHint.LAYERS_3D_PHYSICS
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportFlags3dPhysics

/**
 * Export an integer property as a bit flag field for 3D render layers.
 *
 * The widget in the Inspector dock will use the layer names defined in
 * `ProjectSettings.layer_names/3d_render/layer_1`.
 *
 * @see PropertyHint.LAYERS_3D_RENDER
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportFlags3dRender

/**
 * Export an integer property as a bit flag field for navigation avoidance layers.
 *
 * The widget in the Inspector dock will use the layer names defined in
 * `ProjectSettings.layer_names/avoidance/layer_1`.
 *
 * @see PropertyHint.LAYERS_AVOIDANCE
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportFlagsAvoidance

/**
 * Export a [String], [io.github.kingg22.godot.api.builtin.GodotArray] of String,
 * or [io.github.kingg22.godot.api.builtin.PackedStringArray] property as an absolute path
 * to a directory.
 *
 * The path can be picked from the entire filesystem.
 * See [ExportDir] to limit it to the project folder and its subfolders.
 *
 * @see PropertyHint.GLOBAL_DIR
 * @see ExportDir
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportGlobalDir

/**
 * Export a [String], [io.github.kingg22.godot.api.builtin.GodotArray] of String,
 * or [io.github.kingg22.godot.api.builtin.PackedStringArray] property as an absolute path
 * to a file.
 *
 * The path can be picked from the entire filesystem.
 * See [ExportFile] to limit it to the project folder and its subfolders.
 *
 * If a filter is provided, only matching files will be available for picking.
 *
 * @param filter File filter patterns (e.g., "*.txt").
 * @see PropertyHint.GLOBAL_FILE
 * @see ExportFile
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportGlobalFile(public vararg val filter: String)

/**
 * Define a new group for the following exported properties.
 *
 * This helps to organize properties in the Inspector dock. Groups can be added with an
 * optional prefix, which would make the group only consider properties that have this prefix.
 * The grouping will break on the first property that doesn't have a prefix. The prefix
 * is also removed from the property's name in the Inspector dock.
 *
 * If no prefix is provided, then every following property will be added to the group.
 * The group ends when the next group or category is defined. You can also force end a
 * group by using this annotation with empty strings for parameters.
 *
 * Groups cannot be nested, use [ExportSubgroup] to add subgroups within groups.
 *
 * @param name The name of the group displayed in the Inspector.
 * @param prefix The prefix that properties must have to be included in this group.
 * @see Export
 * @see ExportSubgroup
 * @see ExportCategory
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportGroup(public val name: String, public val prefix: String = "")

/**
 * Export a [String], [io.github.kingg22.godot.api.builtin.GodotArray] of String,
 * [io.github.kingg22.godot.api.builtin.PackedStringArray],
 * [io.github.kingg22.godot.api.builtin.Dictionary] or
 * [io.github.kingg22.godot.api.builtin.GodotArray] of Dictionary property with a large
 * TextEdit widget instead of a LineEdit.
 *
 * This adds support for multiline content and makes it easier to edit a large amount of
 * text stored in the property.
 *
 * @param hint Additional hints for the editor (e.g., "monospace", "no_wrap").
 * @see PropertyHint.MULTILINE_TEXT
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportMultiline(public vararg val hint: String)

/**
 * Export a [io.github.kingg22.godot.api.builtin.NodePath] or
 * [io.github.kingg22.godot.api.builtin.GodotArray] of NodePath property with a filter
 * for allowed node types.
 *
 * Note: The type must be a native class or a globally registered script (using the
 * class_name keyword) that inherits Node.
 *
 * @param type Allowed node type names.
 * @see PropertyHint.NODE_PATH_VALID_TYPES
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportNodePath(public vararg val type: String)

/**
 * Export a [String], [io.github.kingg22.godot.api.builtin.GodotArray] of String,
 * or [io.github.kingg22.godot.api.builtin.PackedStringArray] property with a placeholder
 * text displayed in the editor widget when no value is present.
 *
 * @param placeholder The placeholder text to display.
 * @see PropertyHint.PLACEHOLDER_TEXT
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportPlaceholder(public val placeholder: String)

/**
 * Export an [Int], [Float][kotlin.Double],
 * [io.github.kingg22.godot.api.builtin.GodotArray] of Int,
 * [io.github.kingg22.godot.api.builtin.GodotArray] of Float,
 * [io.github.kingg22.godot.api.builtin.PackedByteArray],
 * [io.github.kingg22.godot.api.builtin.PackedInt32Array],
 * [io.github.kingg22.godot.api.builtin.PackedInt64Array],
 * [io.github.kingg22.godot.api.builtin.PackedFloat32Array], or
 * [io.github.kingg22.godot.api.builtin.PackedFloat64Array] property as a range value.
 *
 * The range must be defined by min and max, as well as an optional step and a variety
 * of extra hints. The step defaults to 1 for integer properties.
 *
 * If hints "or_greater" and "or_less" are provided, the editor widget will not cap the
 * value at range boundaries. The "exp" hint will make the edited values on range to
 * change exponentially. The "prefer_slider" hint will make integer values use the slider
 * instead of arrows for editing, while "hide_control" will hide the element controlling
 * the value of the editor widget.
 *
 * Hints also allow indicating the units for the edited value. Using `radians_as_degrees`
 * you can specify that the actual value is in radians, but should be displayed in degrees
 * in the Inspector dock. `degrees` allows to add a degree sign as a unit suffix.
 * Finally, a custom suffix can be provided using `suffix:unit`.
 *
 * @param min The minimum value of the range.
 * @param max The maximum value of the range.
 * @param step The step value for the range (defaults to 1.0f).
 * @param extraHints Additional hints for the range editor.
 * @see PropertyHint.RANGE
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportRange(
    public val min: Float,
    public val max: Float,
    public val step: Float = 1.0f,
    public vararg val extraHints: String,
)

/**
 * Export a property with `PROPERTY_USAGE_STORAGE` flag.
 *
 * The property is not displayed in the editor, but it is serialized and stored in the
 * scene or resource file. This can be useful for [Tool] scripts.
 * Also, the property value is copied when
 * [Resource.duplicate()][io.github.kingg22.godot.api.core.refcounted.Resource.duplicate]
 * or [Node.duplicate()][io.github.kingg22.godot.api.core.Node.duplicate] is called, unlike non-exported
 * variables.
 *
 * @see PropertyUsageFlags.STORAGE
 * @see Tool
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportStorage

/**
 * Define a new subgroup for the following exported properties.
 *
 * This helps to organize properties in the Inspector dock. Subgroups work exactly like
 * groups, except they need a parent group to exist.
 *
 * Note: Subgroups cannot be nested, but you can use the slash separator (/) to achieve
 * the desired effect.
 *
 * @param name The name of the subgroup displayed in the Inspector.
 * @param prefix The prefix that properties must have to be included in this subgroup.
 * @see Export
 * @see ExportGroup
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportSubgroup(public val name: String, public val prefix: String = "")

/**
 * Export a [io.github.kingg22.godot.api.builtin.Callable] property as a clickable
 * button with the label text.
 *
 * When the button is pressed, the callable is called.
 *
 * If an icon is specified, it is used to fetch an icon for the button via
 * Control.get_theme_icon(), from the "EditorIcons" theme type. If the icon is omitted, the
 * default "Callable" icon is used instead.
 *
 * Consider using the EditorUndoRedoManager to allow the action to be reverted safely.
 *
 * Note: The property is exported without the PROPERTY_USAGE_STORAGE flag because a
 * Callable cannot be properly serialized and stored in a file.
 *
 * Note: In an exported project neither EditorInterface nor EditorUndoRedoManager exist,
 * which may cause some scripts to break.
 *
 * Note: Avoid storing lambda callables in member variables of RefCounted-based classes
 * (e.g., resources), as this can lead to memory leaks. Use only method callables and
 * optionally Callable.bind() or Callable.unbind().
 *
 * @param text The label text for the button.
 * @param icon The name of the icon to use (from the EditorIcons theme).
 * @see PropertyHint.TOOL_BUTTON
 */
@Retention(SOURCE)
@Target(PROPERTY)
@MustBeDocumented
public annotation class ExportToolButton(public val text: String, public val icon: String = "")
