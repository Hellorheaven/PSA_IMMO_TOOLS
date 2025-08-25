// strings/src/main/java/com/helly/psaimmotool/strings/StringRepository.kt
package com.helly.psaimmotool.strings

interface StringRepository {
    fun get(key: String): String
    fun get(key: String, vararg args: Any): String
}