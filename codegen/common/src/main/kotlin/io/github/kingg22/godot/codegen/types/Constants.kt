package io.github.kingg22.godot.codegen.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

private val K_JVM_NAME = ClassName("kotlin.jvm", "JvmName")
private val K_JVM_STATIC = ClassName("kotlin.jvm", "JvmStatic")
val K_DEPRECATED = ClassName("kotlin", "Deprecated")
val K_REPLACE_WITH = ClassName("kotlin", "ReplaceWith")
val K_SUPPRESS = ClassName("kotlin", "Suppress")
val K_AUTOCLOSEABLE = ClassName("kotlin", "AutoCloseable")
val K_TODO = MemberName("kotlin", "TODO")
val K_OPT_IN = ClassName("kotlin", "OptIn")
val K_REQUIRE_NOT_NULL = MemberName("kotlin", "requireNotNull")
val K_CHECK_NOT_NULL = MemberName("kotlin", "checkNotNull")
val K_IGNORABLE_RETURNS = ClassName("kotlin", "IgnorableReturnValue")
