/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package io.github.kingg22.godot.codegen

/** does the string str start like an option? */
private fun isOption(str: String): Boolean = str.length > 1 && str[0] == '-'

/** does the string str start like single char option? */
private fun isSingleCharOptionWithArg(str: String): Boolean {
    assert(isOption(str))
    return str.length > 2 && str[1] != '-'
}

// option part of single char option
// -lclang => -l, -DFOO -> -D
private fun singleCharOption(str: String): String {
    assert(isSingleCharOptionWithArg(str))
    return str.substring(0, 2)
}

// argument part of single char option
// -lclang => clang, -DFOO -> FOO
private fun singleCharOptionArg(str: String): String {
    assert(isSingleCharOptionWithArg(str))
    return str.substring(2)
}

class OptionParser private constructor(
    /** option name to corresponding OptionSpec mapping */
    private val optionSpecs: Map<String, OptionSpec>,
) {
    companion object {
        @JvmStatic
        fun builder(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        private val optionSpecs = HashMap<String, OptionSpec>()

        fun accepts(name: String, help: String?, argRequired: Boolean) {
            accepts(name, mutableListOf(), help, argRequired)
        }

        fun accepts(name: String, aliases: List<String>, help: String?, argRequired: Boolean) {
            val spec = OptionSpec(name, aliases, help, argRequired)
            optionSpecs[name] = spec
            for (alias in aliases) {
                optionSpecs[alias] = spec
            }
        }

        fun build() = OptionParser(optionSpecs)
    }

    fun parse(args: List<String>): OptionSet {
        val options = HashMap<String, MutableList<String>>()
        val nonOptionArgs = ArrayList<String>()
        var index = 0

        while (index < args.size) {
            val arg = args[index]
            // does this look like an option?
            if (isOption(arg)) {
                var spec = optionSpecs[arg]
                var argValue: String? = null
                // does not match known options directly.
                // check for single char option followed
                // by option value without whitespace in between.
                // Examples: -lclang, -DFOO
                if (spec == null) {
                    spec = if (isSingleCharOptionWithArg(arg)) optionSpecs[singleCharOption(arg)] else null
                    // we have a matching single char option and that requires argument
                    if (spec != null && spec.argRequired) {
                        argValue = singleCharOptionArg(arg)
                    } else {
                        // single char option special handling also failed. give up.
                        throw OptionException("invalid option: $arg")
                    }
                }

                // handle argument associated with the current option, if any
                val values: MutableList<String> = if (spec.argRequired) {
                    if (argValue == null) {
                        if (index == args.size - 1) {
                            throw OptionException(spec.help)
                        }
                        argValue = args[index + 1]
                        index++ // consume value from next command line arg
                    } // else -DFOO like case. argValue already set

                    // do not allow argument value to start with '-'
                    // this will catch issues like "-l-lclang", "-l -t"
                    if (argValue[0] == '-') {
                        throw OptionException(spec.help)
                    }
                    options.getOrDefault(spec.name, mutableListOf()).apply {
                        add(argValue)
                    }
                } else {
                    // no argument value associated with this option.
                    // using empty list to flag that.
                    mutableListOf()
                }

                // set value for the option as well as all its aliases
                // so that option lookup, value lookup will work regardless
                // which alias was used to check.
                options[spec.name] = values
                spec.aliases.onEach { _ ->
                    options[spec.name] = values
                }
            } else { // !isOption(arg)
                nonOptionArgs.add(arg)
            }
            index++
        }
        return OptionSet(options, nonOptionArgs)
    }

    class OptionException(msg: String?) : RuntimeException(msg)

    // specification for an option
    @JvmRecord
    private data class OptionSpec(
        val name: String,
        val aliases: List<String>,
        val help: String?,
        val argRequired: Boolean,
    )

    /** output of OptionParser.parse */
    data class OptionSet(
        private val options: Map<String, List<String>>,
        /** non-option arguments */
        private val nonOptionArgs: List<String>,
    ) {
        fun has(name: String?) = options.containsKey(name)

        fun valuesOf(name: String?) = options[name]

        fun valueOf(name: String?): String? {
            val values = valuesOf(name)
            return values?.get(values.size - 1)
        }

        fun nonOptionArguments() = nonOptionArgs
    }
}
