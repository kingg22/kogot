#!/bin/bash

## Dump in txt file the includes symbols
jextract --dump-includes godot_includes.txt gdextension_interface.h

## After that, remove unwanted symbols

## Generate new raw binding filtered
jextract @godot_includes.txt \
--target-package io.github.kingg22.godot.internal.ffm \
--header-class-name FFMUtils \
--output <path> \
gdextension_interface.h
