// Shared stand-in for the generated Godot API (see GodotNative.kt): the real engine class hierarchy.
package io.github.kingg22.godot.api.core

import io.github.kingg22.godot.api.GodotNative

open class GodotObject : GodotNative
open class Node : GodotObject()
open class CanvasItem : Node()
open class Node2D : CanvasItem()
open class Sprite2D : Node2D()
open class RefCounted : GodotObject()
open class Resource : RefCounted()
