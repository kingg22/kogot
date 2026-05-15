package io.github.kingg22.godot.codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

val K_DEPRECATED = ClassName("kotlin", "Deprecated")
val K_REPLACE_WITH = ClassName("kotlin", "ReplaceWith")
val K_SUPPRESS = ClassName("kotlin", "Suppress")
val K_AUTOCLOSEABLE = ClassName("kotlin", "AutoCloseable")
val K_TODO = MemberName("kotlin", "TODO")
val K_OPT_IN = ClassName("kotlin", "OptIn")
val K_ERROR = MemberName("kotlin", "error")
val K_REQUIRE = MemberName("kotlin", "require")
val K_REQUIRE_NOT_NULL = MemberName("kotlin", "requireNotNull")
val K_CHECK = MemberName("kotlin", "check")
val K_CHECK_NOT_NULL = MemberName("kotlin", "checkNotNull")
val K_IGNORABLE_RETURNS = ClassName("kotlin", "IgnorableReturnValue")

val lazyMethod = MemberName("kotlin", "lazy")
