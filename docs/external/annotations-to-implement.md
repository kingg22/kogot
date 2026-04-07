# GDScript Annotations to Implement

This document lists the GDScript annotations from Godot documentation that need to be implemented in Kotlin.

## Excluded Annotations
- `@Signal` - Already has distinct Kotlin API in `io.github.kingg22.godot.api.signal`
- `@onready` - Implemented as `_onReady` method pattern in Node.kt

---

## Property Export Annotations

### @Export

Mark the following property as exported (editable in the Inspector dock and saved to disk). To control the type of the exported property, use the type hint notation.

Note: Custom resources and nodes should be registered as global classes using `class_name`, since the Inspector currently only supports global classes. Otherwise, a less specific type will be exported instead.

Note: Node export is only supported in Node-derived classes and has a number of other limitations.

---

### @ExportCategory

Define a new category for the following exported properties. This helps to organize properties in the Inspector dock.

Note: Categories in the Inspector dock's list usually divide properties coming from different classes. For better clarity, it's recommended to use @export_group and @export_subgroup instead.

---

### @ExportColorNoAlpha

Export a Color, Array[Color], or PackedColorArray property without allowing its transparency (Color.a) to be edited.

---

### @ExportCustom

Allows you to set a custom hint, hint string, and usage flags for the exported property. Note that there's no validation done in GDScript, it will just pass the parameters to the editor.

Note: Regardless of the usage value, the PROPERTY_USAGE_SCRIPT_VARIABLE flag is always added, as with any explicitly declared script variable.

---

### @ExportDir

Export a String, Array[String], or PackedStringArray property as a path to a directory. The path will be limited to the project folder and its subfolders. See @export_global_dir to allow picking from the entire filesystem.

---

### @ExportEnum

Export an int, String, Array[int], Array[String], PackedByteArray, PackedInt32Array, PackedInt64Array, or PackedStringArray property as an enumerated list of options (or an array of options). If the property is an int, then the index of the value is stored. If the property is a String, then the value is stored.

If you want to use named GDScript enums, then use @export instead.

---

### @ExportExpEasing

Export a floating-point property with an easing editor widget. Additional hints can be provided to adjust the behavior of the widget. "attenuation" flips the curve, which makes it more intuitive for editing attenuation properties. "positive_only" limits values to only be greater than or equal to zero.

---

### @ExportFile

Export a String, Array[String], or PackedStringArray property as a path to a file. The path will be limited to the project folder and its subfolders. See @export_global_file to allow picking from the entire filesystem.

If filter is provided, only matching files will be available for picking.

Note: The file will be stored and referenced as UID, if available. This ensures that the reference is valid even when the file is moved.

---

### @ExportFilePath

Same as @export_file, except the file will be stored as a raw path. This means that it may become invalid when the file is moved. If you are exporting a Resource path, consider using @export_file instead.

---

### @ExportFlags

Export an integer property as a bit flag field. This allows to store several "checked" or true values with one property, and comfortably select them from the Inspector dock.

You can add explicit values using a colon. You can also combine several flags.

Note: A flag value must be at least 1 and at most 2^32 - 1.

---

### @ExportFlags2dNavigation

Export an integer property as a bit flag field for 2D navigation layers. The widget in the Inspector dock will use the layer names defined in ProjectSettings.

---

### @ExportFlags2dPhysics

Export an integer property as a bit flag field for 2D physics layers. The widget in the Inspector dock will use the layer names defined in ProjectSettings.

---

### @ExportFlags2dRender

Export an integer property as a bit flag field for 2D render layers. The widget in the Inspector dock will use the layer names defined in ProjectSettings.

---

### @ExportFlags3dNavigation

Export an integer property as a bit flag field for 3D navigation layers. The widget in the Inspector dock will use the layer names defined in ProjectSettings.

---

### @ExportFlags3dPhysics

Export an integer property as a bit flag field for 3D physics layers. The widget in the Inspector dock will use the layer names defined in ProjectSettings.

---

### @ExportFlags3dRender

Export an integer property as a bit flag field for 3D render layers. The widget in the Inspector dock will use the layer names defined in ProjectSettings.

---

### @ExportFlagsAvoidance

Export an integer property as a bit flag field for navigation avoidance layers. The widget in the Inspector dock will use the layer names defined in ProjectSettings.

---

### @ExportGlobalDir

Export a String, Array[String], or PackedStringArray property as an absolute path to a directory. The path can be picked from the entire filesystem. See @export_dir to limit it to the project folder and its subfolders.

---

### @ExportGlobalFile

Export a String, Array[String], or PackedStringArray property as an absolute path to a file. The path can be picked from the entire filesystem. See @export_file to limit it to the project folder and its subfolders.

If filter is provided, only matching files will be available for picking.

---

### @ExportGroup

Define a new group for the following exported properties. This helps to organize properties in the Inspector dock. Groups can be added with an optional prefix, which would make group to only consider properties that have this prefix.

If no prefix is provided, then every following property will be added to the group. The group ends when the next group or category is defined. You can also force end a group by using this annotation with empty strings for parameters.

Groups cannot be nested, use @export_subgroup to add subgroups within groups.

---

### @ExportMultiline

Export a String, Array[String], PackedStringArray, Dictionary or Array[Dictionary] property with a large TextEdit widget instead of a LineEdit. This adds support for multiline content and makes it easier to edit large amount of text stored in the property.

---

### @ExportNodePath

Export a NodePath or Array[NodePath] property with a filter for allowed node types.

Note: The type must be a native class or a globally registered script (using the class_name keyword) that inherits Node.

---

### @ExportPlaceholder

Export a String, Array[String], or PackedStringArray property with a placeholder text displayed in the editor widget when no value is present.

---

### @ExportRange

Export an int, float, Array[int], Array[float], PackedByteArray, PackedInt32Array, PackedInt64Array, PackedFloat32Array, or PackedFloat64Array property as a range value. The range must be defined by min and max, as well as an optional step and a variety of extra hints.

If hints "or_greater" and "or_less" are provided, the editor widget will not cap the value at range boundaries. The "exp" hint will make the edited values on range to change exponentially. The "prefer_slider" hint will make integer values use the slider instead of arrows for editing, while "hide_control" will hide the element controlling the value of the editor widget.

Hints also allow to indicate the units for the edited value. Using "radians_as_degrees" you can specify that the actual value is in radians, but should be displayed in degrees in the Inspector dock. "degrees" allows to add a degree sign as a unit suffix. Finally, a custom suffix can be provided using "suffix:unit".

---

### @ExportStorage

Export a property with PROPERTY_USAGE_STORAGE flag. The property is not displayed in the editor, but it is serialized and stored in the scene or resource file. This can be useful for @tool scripts. Also the property value is copied when Resource.duplicate() or Node.duplicate() is called, unlike non-exported variables.

---

### @ExportSubgroup

Define a new subgroup for the following exported properties. This helps to organize properties in the Inspector dock. Subgroups work exactly like groups, except they need a parent group to exist. See @export_group.

Note: Subgroups cannot be nested, but you can use the slash separator (/) to achieve the desired effect.

---

### @ExportToolButton

Export a Callable property as a clickable button with the label text. When the button is pressed, the callable is called.

If icon is specified, it is used to fetch an icon for the button via Control.get_theme_icon(), from the "EditorIcons" theme type. If icon is omitted, the default "Callable" icon is used instead.

Consider using the EditorUndoRedoManager to allow the action to be reverted safely.

Note: The property is exported without the PROPERTY_USAGE_STORAGE flag because a Callable cannot be properly serialized and stored in a file.

Note: In an exported project neither EditorInterface nor EditorUndoRedoManager exist, which may cause some scripts to break.

Note: Avoid storing lambda callables in member variables of RefCounted-based classes (e.g. resources), as this can lead to memory leaks. Use only method callables and optionally Callable.bind() or Callable.unbind().

---

## Class Annotations

### @Icon

Add a custom icon to the current script. The icon specified at icon_path is displayed in the Scene dock for every node of that class, as well as in various editor dialogs.

Note: Only the script can have a custom icon. Inner classes are not supported.

Note: As annotations describe their subject, the @icon annotation must be placed before the class definition and inheritance.

Note: Unlike most other annotations, the argument of the @icon annotation must be a string literal (constant expressions are not supported).

---

### @StaticUnload

Make a script with static variables to not persist after all references are lost. If the script is loaded again the static variables will revert to their default values.

Note: As annotations describe their subject, the @static_unload annotation must be placed before the class definition and inheritance.

Warning: Currently, due to a bug, scripts are never freed, even if @static_unload annotation is used.

---

### @Tool

Mark the current script as a tool script, allowing it to be loaded and executed by the editor.

Note: As annotations describe their subject, the @tool annotation must be placed before the class definition and inheritance.

---

## Method Annotations

### @Rpc

Mark the following method for remote procedure calls.

If mode is set as "any_peer", allows any peer to call this RPC function. Otherwise, only the authority peer is allowed to call it and mode should be kept as "authority".

If sync is set as "call_remote", the function will only be executed on the remote peer, but not locally. To run this function locally too, set sync to "call_local".

The transfer_mode accepted values are "unreliable", "unreliable_ordered", or "reliable". The transfer_channel defines the channel of the underlying MultiplayerPeer.

Note: Methods annotated with @rpc cannot receive objects which define required parameters in Object._init().

---

## See Also

- @GlobalScope.PROPERTY_HINT_* constants
- @GlobalScope.PROPERTY_USAGE_* constants
