/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException
import java.io.Reader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

// This is verbatim copy of com.sun.tools.javac.main.CommandLine except for package name

private const val NUL = 0.toChar()

/**
 * Various utility methods for processing Java tool command line arguments.
 *
 *
 * **This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.**
 */
object CommandLine {
    /**
     * Process Win32-style command files for the specified command line
     * arguments and return the resulting arguments. A command file argument
     * is of the form '@file' where 'file' is the name of the file whose
     * contents are to be parsed for additional arguments. The contents of
     * the command file are parsed using StreamTokenizer and the original
     * '@file' argument replaced with the resulting tokens. Recursive command
     * files are not supported. The '@' character itself can be quoted with
     * the sequence '@@'.
     * @param args the arguments that may contain @files
     * @return the arguments, with @files expanded
     * @throws IOException if there is a problem reading any of the @files
     */
    @Throws(IOException::class)
    fun parse(args: MutableList<String>): List<String> {
        val newArgs = ArrayList<String>()
        appendParsedCommandArgs(newArgs, args)
        return newArgs
    }

    @Throws(IOException::class)
    private fun appendParsedCommandArgs(newArgs: MutableList<String>, args: List<String>) {
        for (arg in args) {
            var arg = arg
            if (arg.length > 1 && arg[0] == '@') {
                arg = arg.substring(1)
                if (arg[0] == '@') {
                    newArgs.add(arg)
                } else {
                    loadCmdFile(arg, newArgs)
                }
            } else {
                newArgs.add(arg)
            }
        }
    }

    /**
     * Process the given environment variable and appends any Win32-style
     * command files for the specified command line arguments and return
     * the resulting arguments. A command file argument
     * is of the form '@file' where 'file' is the name of the file whose
     * contents are to be parsed for additional arguments. The contents of
     * the command file are parsed using StreamTokenizer and the original
     * '@file' argument replaced with the resulting tokens. Recursive command
     * files are not supported. The '@' character itself can be quoted with
     * the sequence '@@'.
     * @param envVariable the env variable to process
     * @param args the arguments that may contain @files
     * @return the arguments, with environment variable's content and expansion of @files
     * @throws IOException if there is a problem reading any of the @files
     * @throws UnmatchedQuote
     */
    @Throws(IOException::class, UnmatchedQuote::class)
    fun parse(envVariable: String?, args: List<String>): List<String> {
        val inArgs = ArrayList<String>()
        appendParsedEnvVariables(inArgs, envVariable)
        inArgs.addAll(args)
        val newArgs = ArrayList<String>()
        appendParsedCommandArgs(newArgs, inArgs)
        return newArgs
    }

    @Throws(IOException::class)
    private fun loadCmdFile(name: String, args: MutableList<String>) {
        Files.newBufferedReader(Paths.get(name), Charset.defaultCharset()).use { r ->
            val t = Tokenizer(r)
            var token = t.nextToken()
            while (token != null) {
                args.add(token)
                token = t.nextToken()
            }
        }
    }

    @Throws(UnmatchedQuote::class)
    private fun appendParsedEnvVariables(newArgs: MutableList<String>, envVariable: String?) {
        if (envVariable == null) return

        val `in` = System.getenv(envVariable)
        if (`in` == null || `in`.trim { it <= ' ' }.isEmpty()) {
            return
        }

        val len = `in`.length

        var pos = 0
        val sb = StringBuilder()
        var quote = NUL
        var ch: Char

        loop@ while (pos < len) {
            ch = `in`[pos]
            when (ch) {
                '\"', '\'' -> {
                    when (quote) {
                        NUL -> {
                            quote = ch
                        }

                        ch -> {
                            quote = NUL
                        }

                        else -> {
                            sb.append(ch)
                        }
                    }
                    pos++
                }

                '\u000c', '\n', '\r', '\t', ' ' -> {
                    if (quote == NUL) {
                        newArgs.add(sb.toString())
                        sb.setLength(0)
                        while (ch == '\u000c' || ch == '\n' || ch == '\r' || ch == '\t' || ch == ' ') {
                            pos++
                            if (pos >= len) {
                                break@loop
                            }
                            ch = `in`.get(pos)
                        }
                        break
                    }
                    sb.append(ch)
                    pos++
                }

                else -> {
                    sb.append(ch)
                    pos++
                }
            }
        }
        if (sb.isNotEmpty()) {
            newArgs.add(sb.toString())
        }
        if (quote != NUL) {
            throw UnmatchedQuote(envVariable)
        }
    }

    private class Tokenizer(private val `in`: Reader) {
        private var ch: Int

        init {
            ch = `in`.read()
        }

        @Throws(IOException::class)
        fun nextToken(): String? {
            skipWhite()
            if (ch == -1) {
                return null
            }

            val sb = StringBuilder()
            var quoteChar = 0.toChar()

            while (ch != -1) {
                when (ch) {
                    ' '.code, '\t'.code, '\u000c'.code -> {
                        if (quoteChar.code == 0) {
                            return sb.toString()
                        }
                        sb.append(ch.toChar())
                    }

                    '\n'.code, '\r'.code -> return sb.toString()

                    '\''.code, '"'.code -> when (quoteChar.code) {
                        0 -> {
                            quoteChar = ch.toChar()
                        }

                        ch -> {
                            quoteChar = 0.toChar()
                        }

                        else -> {
                            sb.append(ch.toChar())
                        }
                    }

                    '\\'.code -> {
                        if (quoteChar.code != 0) {
                            ch = `in`.read()
                            when (ch) {
                                '\n'.code, '\r'.code -> {
                                    while (ch == ' '.code || ch == '\n'.code || ch == '\r'.code || ch == '\t'.code ||
                                        ch == '\u000c'.code
                                    ) {
                                        ch = `in`.read()
                                    }
                                    continue
                                }

                                'n'.code -> ch = '\n'.code

                                'r'.code -> ch = '\r'.code

                                't'.code -> ch = '\t'.code

                                'f'.code -> ch = '\u000c'.code
                            }
                        }
                        sb.append(ch.toChar())
                    }

                    else -> sb.append(ch.toChar())
                }

                ch = `in`.read()
            }

            return sb.toString()
        }

        @Throws(IOException::class)
        fun skipWhite() {
            while (ch != -1) {
                when (ch) {
                    ' '.code, '\t'.code, '\n'.code, '\r'.code, '\u000c'.code -> {}

                    '#'.code -> {
                        ch = `in`.read()
                        while (ch != '\n'.code && ch != '\r'.code && ch != -1) {
                            ch = `in`.read()
                        }
                    }

                    else -> return
                }

                ch = `in`.read()
            }
        }
    }

    class UnmatchedQuote(val variableName: String?) : Exception()
}
