// Minimal GDExtension that asks Godot itself which engine classes an extension class can inherit, with no kogot code
// involved. Registers one extension class "KX_<Parent>" per parent listed in $KX_PARENTS (one Godot class name per
// line), the way kogot's createInstanceFunc does: classdb_construct_object3(parent) + object_set_instance.
//   KX_LEVEL     initialization level to register at (default 2 = SCENE; 3 = EDITOR, for the editor classes)
//   KX_POSTINIT  when set, sends NOTIFICATION_POSTINITIALIZE as kogot does (some nodes crash without it)
// Each registration prints "[kx] registered? KX_<Parent> yes|no"; Godot logs the reason of every "no".
// Build: gcc -shared -fPIC -I<godot-version/v4_7_2> -o bin/libkx.so matrix.c
// See docs/inheritance-matrix.md for the project layout and the results.
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "gdextension_interface.h"

static GDExtensionInterfaceGetProcAddress gpa;
static GDExtensionClassLibraryPtr lib;
static GDExtensionInterfaceStringNameNewWithUtf8Chars sn_new;
static GDExtensionInterfaceClassdbConstructObject3 construct3;
static GDExtensionInterfaceObjectSetInstance set_instance;
static GDExtensionInterfaceClassdbRegisterExtensionClass5 reg5;
static GDExtensionInterfaceClassdbUnregisterExtensionClass unreg;
static GDExtensionInterfaceClassdbGetClassTag get_tag;

typedef struct { void *p[2]; } SN;
typedef struct { SN parent, self; char name[128]; } Entry;
static Entry *entries; static int count;
static int dummy;
static GDExtensionInterfaceClassdbGetMethodBind get_mb;
static GDExtensionInterfaceObjectMethodBindPtrcall ptrcall;
static GDExtensionMethodBindPtr notif_mb;

static GDExtensionObjectPtr create_instance(void *ud, GDExtensionBool post) {
    Entry *e = ud;
    GDExtensionObjectPtr o = construct3(&e->parent);
    if (!o) { fprintf(stderr, "[kx] construct %s returned NULL\n", e->name); return NULL; }
    set_instance(o, &e->self, &dummy);
    if (post && getenv("KX_POSTINIT")) {
        if (!notif_mb) { SN c, m; sn_new(&c, "Object"); sn_new(&m, "notification"); notif_mb = get_mb(&c, &m, 4023243586); }
        int64_t what = 0; GDExtensionBool rev = 0; /* NOTIFICATION_POSTINITIALIZE = 0 */
        const void *args[2] = { &what, &rev };
        ptrcall(notif_mb, o, args, NULL);
    }
    return o;
}
static void free_instance(void *ud, GDExtensionClassInstancePtr inst) {}
static GDExtensionClassCallVirtual get_virtual(void *ud, GDExtensionConstStringNamePtr name, uint32_t hash) { return NULL; }

static void load(void) {
    FILE *f = fopen(getenv("KX_PARENTS"), "r");
    char line[128]; int cap = 2048;
    entries = calloc(cap, sizeof(Entry));
    while (fgets(line, sizeof line, f)) {
        line[strcspn(line, "\r\n")] = 0;
        if (!*line) continue;
        strcpy(entries[count++].name, line);
    }
    fclose(f);
}
static int want_level(void) { return getenv("KX_LEVEL") ? atoi(getenv("KX_LEVEL")) : GDEXTENSION_INITIALIZATION_SCENE; }
static void init(void *ud, GDExtensionInitializationLevel level) {
    if ((int)level != want_level()) return;
    load();
    for (int i = 0; i < count; i++) {
        Entry *e = &entries[i];
        char self[160]; snprintf(self, sizeof self, "KX_%s", e->name);
        sn_new(&e->parent, e->name);
        sn_new(&e->self, self);
        GDExtensionClassCreationInfo5 info; memset(&info, 0, sizeof info);
        info.is_exposed = 1;
        info.create_instance_func = create_instance;
        info.free_instance_func = free_instance;
        info.get_virtual_func = get_virtual;
        info.class_userdata = e;
        reg5(lib, &e->self, &e->parent, &info);
        fprintf(stderr, "[kx] registered? %s %s\n", self, get_tag(&e->self) ? "yes" : "no");
    }
}
static void deinit(void *ud, GDExtensionInitializationLevel level) {
    if ((int)level != want_level()) return;
    for (int i = count - 1; i >= 0; i--) if (get_tag(&entries[i].self)) unreg(lib, &entries[i].self);
}
GDExtensionBool kx_init(GDExtensionInterfaceGetProcAddress p, GDExtensionClassLibraryPtr l, GDExtensionInitialization *r) {
    gpa = p; lib = l;
    sn_new = (void *)gpa("string_name_new_with_utf8_chars");
    construct3 = (void *)gpa("classdb_construct_object3");
    set_instance = (void *)gpa("object_set_instance");
    reg5 = (void *)gpa("classdb_register_extension_class5");
    unreg = (void *)gpa("classdb_unregister_extension_class");
    get_tag = (void *)gpa("classdb_get_class_tag");
    get_mb = (void *)gpa("classdb_get_method_bind");
    ptrcall = (void *)gpa("object_method_bind_ptrcall");
    r->initialize = init; r->deinitialize = deinit;
    r->minimum_initialization_level = GDEXTENSION_INITIALIZATION_SCENE;
    return 1;
}
