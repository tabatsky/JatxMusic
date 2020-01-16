package jatx.debug

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

object AppDebug {
    fun setAppCrashHandler() {
        val oldHandler =
            Thread.getDefaultUncaughtExceptionHandler()
        if (oldHandler is AppCrashHandler) {
            Log.e("crash handler", "already set")
            return
        }
        Thread.setDefaultUncaughtExceptionHandler(
            AppCrashHandler(
                oldHandler
            )
        )
    }
}

class AppCrashHandler(private val oldHandler: Thread.UncaughtExceptionHandler) :
    Thread.UncaughtExceptionHandler {

    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable
    ) {
        Log.e("app crash", exceptionToString(throwable))
        Thread.sleep(30000)
        oldHandler.uncaughtException(thread, throwable)
    }
}

fun exceptionToString(e: Throwable): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    e.printStackTrace(pw)
    return sw.toString()
}

fun logError(e: Throwable) {
    Log.e("error", exceptionToString(e))
}