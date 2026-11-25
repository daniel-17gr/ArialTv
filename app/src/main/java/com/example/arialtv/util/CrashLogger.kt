package com.example.arialtv.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.arialtv.data.model.AppMetadata
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight local uncaught-exception recorder for on-device Android TV diagnostics.
 */
object CrashLogger {
    private const val CRASH_FILE = "last_crash.txt"
    private const val MAX_LOG_CHARS = 12 * 1024

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())
                val stackTrace = Log.getStackTraceString(throwable)
                val report = buildString {
                    appendLine("Time: $timestamp")
                    appendLine("App: ${AppMetadata.APP_NAME} v${AppMetadata.VERSION_NAME}")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    append(stackTrace)
                }.take(MAX_LOG_CHARS)

                File(appContext.filesDir, CRASH_FILE).writeText(report, Charsets.UTF_8)
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun readLastCrash(context: Context): String? = runCatching {
        val file = File(context.applicationContext.filesDir, CRASH_FILE)
        if (!file.exists()) return@runCatching null
        file.readText(Charsets.UTF_8).trim().takeIf { it.isNotBlank() }
    }.getOrNull()

    fun clearLastCrash(context: Context) {
        runCatching {
            val file = File(context.applicationContext.filesDir, CRASH_FILE)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
