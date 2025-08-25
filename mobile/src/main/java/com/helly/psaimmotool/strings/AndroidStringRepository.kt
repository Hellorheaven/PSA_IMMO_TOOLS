// mobile/src/main/java/com/helly/psaimmotool/strings/AndroidStringRepository.kt
package com.helly.psaimmotool.strings

import android.content.Context
import com.helly.psaimmotool.R

class AndroidStringRepository(private val context: Context) : StringRepository {

    private val keys = mapOf(
        "error_network" to R.string.error_network,
        "status_connected" to R.string.status_connected,
        "status_disconnected" to R.string.status_disconnected,
    )

    override fun get(key: String): String {
        val resId = keys[key] ?: error("String key not found: $key")
        return context.getString(resId)
    }

    override fun get(key: String, vararg args: Any): String {
        val resId = keys[key] ?: error("String key not found: $key")
        return context.getString(resId, *args)
    }
}
