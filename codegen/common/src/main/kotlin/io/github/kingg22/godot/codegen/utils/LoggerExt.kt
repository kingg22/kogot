package io.github.kingg22.godot.codegen.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.spi.LoggingEventBuilder
import java.lang.invoke.MethodHandles

/**
 * Returns a logger that corresponds to the [T] class.
 */
inline fun <reified T : Any> logger(): Logger = LoggerFactory.getLogger(T::class.java)

/**
 * Returns a logger that corresponds to the [T] class inferred from the receiver.
 *
 * A shortcut to [logger] to avoid writing the type parameter by hand.
 *
 * Note: this method only uses [this] value to infer the type parameter [T].
 * It does not use the **actual** runtime class (`this::class`) of the receiver value.
 */
inline fun <reified T : Any> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

/**
 * Returns a logger that corresponds to the class of the caller method.
 *
 * Useful for getting logger for global functions without passing a class or package
 *
 * This function MUST be inline to properly get the calling class.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun currentClassLogger(): Logger {
    val clazz = MethodHandles.lookup().lookupClass()
    return LoggerFactory.getLogger(clazz)
}

/**
 * Returns a logger corresponding to the current file if called in a global context like a global function or a global initializer.
 *
 * It returns a logger with a real class category if it's called inside a real class. No checks are being performed on this.
 *
 * A shortcut to [currentClassLogger].
 *
 * This function MUST be inline to properly get the calling class.
 *
 * Example:
 * ```
 * // file level member
 * private val logger = fileLogger()
 * ```
 */
@Suppress("NOTHING_TO_INLINE")
inline fun fileLogger(): Logger = currentClassLogger()

inline fun Logger.trace(t: Throwable? = null, block: () -> String) {
    if (isTraceEnabled) trace(block(), t)
}

inline fun Logger.atTrace(autoFlush: Boolean = true, block: LoggingEventBuilder.() -> Unit) {
    atTrace().apply {
        block()
        if (autoFlush) log()
    }
}

inline fun Logger.debug(t: Throwable? = null, block: () -> String) {
    if (isDebugEnabled) debug(block(), t)
}

inline fun Logger.atDebug(autoFlush: Boolean = true, block: LoggingEventBuilder.() -> Unit) {
    atDebug().apply {
        block()
        if (autoFlush) log()
    }
}

inline fun Logger.info(t: Throwable? = null, block: () -> String) {
    if (isInfoEnabled) info(block(), t)
}

inline fun Logger.atInfo(autoFlush: Boolean = true, block: LoggingEventBuilder.() -> Unit) {
    atInfo().apply {
        block()
        if (autoFlush) log()
    }
}

inline fun Logger.warning(t: Throwable? = null, block: () -> String) {
    if (isWarnEnabled) warn(block(), t)
}

inline fun Logger.atWarn(autoFlush: Boolean = true, block: LoggingEventBuilder.() -> Unit) {
    atWarn().apply {
        block()
        if (autoFlush) log()
    }
}

inline fun Logger.error(t: Throwable? = null, block: () -> String) {
    if (isErrorEnabled) error(block(), t)
}

inline fun Logger.atError(autoFlush: Boolean = true, block: LoggingEventBuilder.() -> Unit) {
    atError().apply {
        block()
        if (autoFlush) log()
    }
}

fun LoggingEventBuilder.addArguments(vararg args: Any?) {
    args.forEach { addArgument(it) }
}
