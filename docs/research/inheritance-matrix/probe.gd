# Instantiates `<KX_PREFIX><Parent>` for every parent listed in $KX_PARENTS (one Godot class name per line) and prints
# one `KX|index|parent|registered-or-not|ok/null/skipped|class/is_parent` line per parent.
#   KX_PREFIX  class prefix: KX_ for matrix.c, KK_ for the kogot InheritanceMatrix (default KX_)
#   KX_START   first index to test (drive.py resumes after a crash with it)
#   KX_SKIP    comma-separated parents to check for registration only
# The last `KX-TRY` line on stderr names the class being instantiated when the engine crashed.
# Usage: godot --headless --path <project> -s probe.gd
extends SceneTree

func _init():
    var names = FileAccess.get_file_as_string(OS.get_environment("KX_PARENTS")).split("\n", false)
    var skip = OS.get_environment("KX_SKIP").split(",", false)
    var start = int(OS.get_environment("KX_START"))
    var prefix = OS.get_environment("KX_PREFIX")
    if prefix == "":
        prefix = "KX_"
    for i in range(start, names.size()):
        var n = names[i]
        var k = prefix + n
        if not ClassDB.class_exists(k):
            print("KX|%d|%s|unregistered|-|-" % [i, n]); continue
        if n in skip:
            print("KX|%d|%s|registered|skipped|-" % [i, n]); continue
        printerr("KX-TRY|%d|%s" % [i, n])
        var o = ClassDB.instantiate(k)
        if o == null:
            print("KX|%d|%s|registered|null|-" % [i, n]); continue
        print("KX|%d|%s|registered|ok|%s/%s" % [i, n, o.get_class(), o.is_class(n)])
        if o is Node: o.free()
        elif not (o is RefCounted): o.free()
        o = null
    print("KX-DONE")
    quit()
