package io.github.kingg22.godot.codegen

import java.io.PrintWriter

class Logger(private val outWriter: PrintWriter, private val errWriter: PrintWriter) {
    private var nErrors = 0
    private var nClangErrors = 0

    fun fatal(t: Throwable, msg: String, vararg args: Any?) {
        errWriter.println(format($$"fatal: %1$s", format(msg, args)))
        t.printStackTrace(errWriter)
    }

    fun err(key: String, vararg args: Any?) {
        val msg = String.format(
            $$"error: %1$s",
            format(key, args),
        )
        errWriter.println(msg)
        nErrors++
    }

    // TODO
    fun format(key: String, vararg args: Any?): String = key.format(args)

    fun hasErrors(): Boolean = nErrors > 0

    fun hasClangErrors(): Boolean = nClangErrors > 0

    fun info(key: String, vararg args: Any?) {
        val msg = format(key, args)
        outWriter.println(msg)
    }

    companion object {
        @JvmStatic
        val DEFAULT: Logger = Logger(
            PrintWriter(System.out, true),
            PrintWriter(System.err, true),
        )
    }
}
