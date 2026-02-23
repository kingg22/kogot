package io.github.kingg22.godot.codegen

import io.github.kingg22.godot.codegen.models.GDExtensionInterface
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.system.exitProcess

// error codes
private const val SUCCESS = 0
private const val FAILURE = 1
private const val OPTION_ERROR = 2
private const val INPUT_ERROR = 3
private const val FATAL_ERROR = 5
private const val OUTPUT_ERROR = 6

private val logger: Logger = Logger.DEFAULT

private fun printOptionError(message: String?) {
    logger.err(
        """{0}\n\
        Usage: jextract <options> <header file>\n
        Use --help for a list of possible options
        """.trimIndent(),
        message,
    )
}

private fun printHelp(exitCode: Int): Int {
    logger.info("TODO make Help message")
    return exitCode
}

private fun run(args: Array<String>): Int {
    val args = try {
        CommandLine.parse(args.toMutableList())
    } catch (ioexp: IOException) {
        logger.fatal(ioexp, "argfile.read.error", ioexp)
        return OPTION_ERROR
    }

    val parser = OptionParser.builder {
        accepts("--input", listOf("-i", "--input-file"), "help.input", true)
        accepts("--output", "help.output", true)
        accepts("--package", listOf("-p"), "help.package", true)

        // optionals
        accepts("-h", listOf("-?", "--help"), "help.h", false)
        accepts("--version", "help.version", false)
    }

    val optionSet = try {
        parser.parse(args)
    } catch (oe: OptionParser.OptionException) {
        printOptionError(oe.message)
        return OPTION_ERROR
    }
/*
    if (optionSet.has("--version")) {
        val version = JextractTool::class.java.getModule().getDescriptor().version()
        logger.info(
            "jextract.version",
            version.get(),
            System.getProperty("java.runtime.version"),
            LibClang.version(),
        )
        return SUCCESS
    }
*/

    if (optionSet.has("-h")) {
        return printHelp(SUCCESS)
    }

    logger.info(optionSet.toString())

    val inputFile = Path(optionSet.valueOf("--input")!!)

    if (!inputFile.exists()) {
        logger.err("file.not.found", inputFile)
        return INPUT_ERROR
    }

    try {
        val json = Json
        val api = json.decodeFromString<GDExtensionInterface>(inputFile.readText())

        println()
        println(api.toString())
    } catch (e: Exception) {
        logger.fatal(e, "file.read.error", inputFile)
        return FATAL_ERROR
    }

    return if (logger.hasErrors()) FAILURE else SUCCESS
}

fun main(args: Array<String>) {
    exitProcess(run(args))
}
