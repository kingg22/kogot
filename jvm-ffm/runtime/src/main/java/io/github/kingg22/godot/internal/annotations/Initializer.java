package io.github.kingg22.godot.internal.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/** Metadata annotation to indicate the given method is an initializer for the class */
@Retention(CLASS)
@Target(METHOD)
public @interface Initializer {}
