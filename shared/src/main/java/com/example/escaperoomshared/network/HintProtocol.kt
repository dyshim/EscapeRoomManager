package com.example.escaperoomshared.network

import com.example.escaperoomshared.model.HintUsageEvent
import java.nio.charset.StandardCharsets

object HintProtocol {
    const val PORT = 45992
    private const val VERSION = "1"
    private const val TYPE = "HINT"
    private const val FIELD_SEPARATOR = "|"

    fun encode(event: HintUsageEvent): ByteArray {
        val message = listOf(
            VERSION,
            TYPE,
            event.roomId,
            event.hintNumber.coerceAtLeast(1).toString(),
            event.usedAtMillis.toString()
        ).joinToString(FIELD_SEPARATOR)
        return message.toByteArray(StandardCharsets.UTF_8)
    }

    fun decode(bytes: ByteArray, length: Int): HintUsageEvent? {
        return runCatching {
            val fields = String(bytes, 0, length, StandardCharsets.UTF_8)
                .split(FIELD_SEPARATOR)
            if (fields.size != 5 || fields[0] != VERSION || fields[1] != TYPE) return null

            HintUsageEvent(
                roomId = fields[2],
                hintNumber = fields[3].toInt().coerceAtLeast(1),
                usedAtMillis = fields[4].toLong()
            )
        }.getOrNull()
    }
}
