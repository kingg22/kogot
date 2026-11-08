// RUN_PIPELINE_TILL: CODEGEN
// Black box: any GodotObject subclass is Variant compatible (OBJECT), however deep the hierarchy, user classes included.

import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.*

interface Damageable

// GodotObject -> Node -> CanvasItem -> Node2D -> Sprite2D -> Enemy -> Boss: seven levels.
open class Enemy : Sprite2D(), Damageable
class Boss : Enemy()
class Player : Node2D(), Damageable
abstract class Weapon : Node2D()
class Sword : Weapon()
class Item : Resource()
class Pool<T> : Node()
object GameManager : Node()

fun engineClasses(node: Node, canvasItem: CanvasItem, node2D: Node2D, sprite: Sprite2D, refCounted: RefCounted, resource: Resource) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(node)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(canvasItem)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(node2D)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(sprite)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(refCounted)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(resource)
}

fun userClasses(enemy: Enemy, boss: Boss?, player: Player, weapon: Weapon, sword: Sword, item: Item, pool: Pool<Regex>) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(enemy)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(boss)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(player)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(weapon)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(sword)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(item)
    // The type argument of a GodotObject subclass is irrelevant: the value is still an OBJECT.
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(pool)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(GameManager)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(Boss())
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(object : Node2D() {})
}

fun explicitTypeArguments() {
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Boss>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Weapon?>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><Pool<Int>>()
}

fun callables(boss: Boss) {
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { target: Boss -> target.toString() }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { attacker: Player, weapon: Weapon -> attacker }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { boss }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { _: Int -> Sword() }
}

// A user interface may be implemented by a GodotObject subclass (Enemy, Player): accepted, checked at runtime.
fun interfaces(damageable: Damageable, nullableDamageable: Damageable?) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(damageable)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(nullableDamageable)
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { target: Damageable -> }
}
