package com.helly.psaimmotool.can

data class CanFrame(
    val id: Int,
    val data: ByteArray
) {
    fun toHexString(): String {
        val idHex = String.format("%03X", id)
        val dataHex = data.joinToString(" ") { "%02X".format(it) }
        return "$idHex $dataHex"
    }

    override fun toString(): String {
        return toHexString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CanFrame

        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + data.contentHashCode()
        return result
    }

    companion object {
        /**
         * Parse une chaîne comme : "7E0 04 27 02 AA BB"
         */
        fun parse(line: String): CanFrame {
            try {
                val parts = line.trim().split(" ")
                val id = parts[0].toInt(16)
                val dataBytes = parts.drop(2).map { it.toInt(16).toByte() }.toByteArray()
                return CanFrame(id, dataBytes)
            } catch (e: Exception) {
                return CanFrame(0, byteArrayOf()) // Valeur vide en cas d'erreur
            }
        }
    }

}
