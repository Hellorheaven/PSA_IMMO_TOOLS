// Fichier : ContextProvider.kt
package com.helly.psaimmotool.utils

import android.content.Context

object ContextProvider {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getString(resId: Int, vararg formatArgs: Any): String {
        return appContext.getString(resId, *formatArgs)
    }
}
