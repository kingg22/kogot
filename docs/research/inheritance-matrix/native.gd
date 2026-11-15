# Instantiates the native class $KX_ONE alone, with no extension subclass, to tell an engine crash from an
# inheritance one. Prints `NATIVE|<class>|ok` or `NATIVE|<class>|null`.
# Usage: godot --headless --path <project> -s native.gd
extends SceneTree

func _init():
    var n = OS.get_environment("KX_ONE")
    var o = ClassDB.instantiate(n)
    print("NATIVE|%s|%s" % [n, "null" if o == null else "ok"])
    if o != null:
        if o is Node: o.free()
        elif not (o is RefCounted): o.free()
    quit()
