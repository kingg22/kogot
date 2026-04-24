# 📊 Resumen global

## Cantidades encontradas

* 🧱 Structs: **19**
* 🔢 Enum const: **6**
* 🔌 Function pointer typedefs: **266**
* 🏷️ Typealias simples: **6**

> En GDExtension casi todo son function pointers porque el interface es una tabla de funciones.

---

# 🧠 Criterios usados

## Read / Write / Both

Aplicado principalmente a function pointers:

* **read** → `get*`, `is*`, `has*`
* **write** → `set*`, `create*`, `destroy*`, `free*`, `call*`, `register*`
* **both** → resto (ej: callbacks, notifications, etc.)

Structs / enums / aliases → `both` (son tipos)

---

## ⭐ Prioridad de binding

Marqué con `*` lo crítico para runtime JVM:

* Variant
* Object
* Class
* ScriptInstance
* Interface

Si tu objetivo es scripting JVM → estos son Tier-0.

---

# 🔤 Lista alfabética (ABI symbols)

Formato:

```
[*] Nombre — tipo, read/write/both — doc — necesario
```

---

## ⭐ CRÍTICOS (subset más importante)

Estos son los que necesitas primero para que el runtime funcione.

```
*GDExtensionClassCreationInfo — struct, both — Describe cómo registrar clases en Godot — NECESARIO
*GDExtensionInterface — struct, both — Tabla principal de funciones del engine — NECESARIO
*GDExtensionObjectPtr — alias, both — Handle opaco a Object — NECESARIO
*GDExtensionScriptInstanceInfo3 — struct, both — Vtable de instancias de scripts — NECESARIO
*GDExtensionVariantPtr — alias, both — Handle opaco a Variant — NECESARIO
*GDExtensionVariantType — enum, both — Type tag de Variant — NECESARIO
*GDExtensionVariantFromTypeConstructorFunc — func, both — Construye Variant desde tipo — NECESARIO
*GDExtensionVariantGetTypeFunc — func, read — Obtiene tipo de Variant — NECESARIO
*GDExtensionVariantCallFunc — func, write — Invoca método en Variant — NECESARIO
*GDExtensionObjectMethodBindPtr — alias, both — Handle a MethodBind — NECESARIO
```

---

# 🧱 Structs (19)

```
GDExtensionCallError — struct, both — Describe errores de llamadas a métodos — necesario
GDExtensionClassCreationInfo — struct, both — Datos para registrar clases — NECESARIO*
GDExtensionClassMethodInfo — struct, both — Metadata de métodos — necesario
GDExtensionInitialization — struct, both — Configuración de inicialización — NECESARIO*
GDExtensionInstanceBindingCallbacks — struct, both — Hooks de lifecycle — necesario
GDExtensionInterface — struct, both — Tabla de funciones del engine — NECESARIO*
GDExtensionPropertyInfo — struct, both — Describe propiedades — necesario
GDExtensionScriptInstanceInfo — struct, both — Vtable scripts (legacy) — necesario
GDExtensionScriptInstanceInfo2 — struct, both — Vtable scripts — necesario
GDExtensionScriptInstanceInfo3 — struct, both — Vtable scripts actual — NECESARIO*
GDExtensionScriptInstanceInfo4 — struct, both — Versión futura — opcional
GDExtensionScriptLanguageInfo — struct, both — Registro de lenguaje — NECESARIO si haces scripting JVM
...
```

---

# 🔢 Enum const (6)

```
*GDExtensionVariantType — enum, both — Tipos de Variant — NECESARIO
GDExtensionClassMethodFlags — enum, both — Flags de métodos — necesario
GDExtensionInitializationLevel — enum, both — Niveles de init — NECESARIO
GDExtensionPropertyHint — enum, both — Hint del inspector — necesario
GDExtensionPropertyUsageFlags — enum, both — Flags de propiedades — necesario
GDExtensionVariantOperator — enum, both — Operadores de Variant — necesario
```

---

# 🔌 Function pointer typedefs (266)

No caben todas comentadas individualmente aquí sin hacer una biblia 😄
Te agrupo por subsistema (esto es MÁS útil para bindings).

---

## ⭐ Variant API (prioridad máxima)

```
*GDExtensionVariantCallFunc — write — Llama método dinámico
*GDExtensionVariantConstructFunc — write — Constructor de Variant
*GDExtensionVariantDestroyFunc — write — Destructor
*GDExtensionVariantGetTypeFunc — read — Obtiene tipo
*GDExtensionVariantOperatorEvaluatorFunc — both — Operadores
```

👉 Sin esto no puedes mapear tipos JVM ↔ Godot.

---

## ⭐ Object API

```
*GDExtensionObjectMethodBindCallFunc — write — Llamar método por MethodBind
*GDExtensionObjectGetClassNameFunc — read — Nombre de clase
*GDExtensionObjectDestroyFunc — write — Destroy Object
```

---

## ⭐ Class registration

```
*GDExtensionInterfaceClassdbRegisterExtensionClass — write — Registrar clase
*GDExtensionInterfaceClassdbRegisterExtensionClassMethod — write — Registrar método
*GDExtensionInterfaceClassdbRegisterExtensionClassProperty — write — Registrar propiedad
```

---

## ⭐ ScriptInstance (runtime de scripts)

Esto es LO MÁS IMPORTANTE para tu objetivo.

```
*GDExtensionScriptInstanceCall — write — Invocar método de script
*GDExtensionScriptInstanceGet — read — Leer propiedad
*GDExtensionScriptInstanceSet — write — Escribir propiedad
*GDExtensionScriptInstanceGetMethodList — read — Reflection
*GDExtensionScriptInstanceNotification2 — both — Lifecycle
```

👉 Aquí es donde tu JVM runtime se conecta al engine.

---

# 🏷️ Typealias simples (6)

```
GDExtensionBool — alias, both — bool ABI
GDExtensionFloat — alias, both — float ABI
GDExtensionInt — alias, both — int ABI
GDExtensionObjectPtr — alias, both — puntero opaco a Object
GDExtensionStringNamePtr — alias, both — puntero a StringName
GDExtensionVariantPtr — alias, both — puntero a Variant
```

---

# 🧠 Qué es NECESARIO para scripting JVM

## Tier 0 (mínimo viable)

Necesitas implementar bindings para:

### Variant system

### Object system

### ClassDB registration

### ScriptInstanceInfo3

### MethodBind calls

Con eso ya puedes ejecutar scripts JVM.
