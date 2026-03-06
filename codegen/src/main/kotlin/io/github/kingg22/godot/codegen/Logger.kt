package io.github.kingg22.godot.codegen

import java.io.PrintWriter

class Logger(
    private val outWriter: PrintWriter = PrintWriter(System.out, true),
    private val errWriter: PrintWriter = PrintWriter(System.err, true),
) {
    fun fatal(t: Throwable, msg: String, vararg args: Any?) {
        val message = msg + args.joinToString(separator = " ")
        errWriter.println(message)
        t.printStackTrace(errWriter)
    }

    fun err(key: String, vararg args: Any?) {
        val msg = key + args.joinToString(separator = " ")
        errWriter.println(msg)
    }

    fun info(key: String, vararg args: Any?) {
        val msg = key + args.joinToString(separator = " ")
        outWriter.println(msg)
    }
}
