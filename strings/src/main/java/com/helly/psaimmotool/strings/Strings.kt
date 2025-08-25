// strings/src/main/java/com/helly/psaimmotool/strings/Strings.kt
package com.helly.psaimmotool.strings

object Strings {
    lateinit var repo: StringRepository

    fun init(repository: StringRepository) {
        repo = repository
    }

    fun get(key: String) = repo.get(key)
    fun format(key: String, vararg args: Any) = repo.get(key, *args)
}
