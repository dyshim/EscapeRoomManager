package com.example.escaperoomshared.network

import com.example.escaperoomshared.model.SharedRoomState
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SyncProtocol {
    const val PORT = 45991
    const val VERSION = "1"
    private const val FIELD_SEPARATOR = "|"

    fun encodeRoom(state: SharedRoomState): ByteArray {
        val encodedName = URLEncoder.encode(state.name, StandardCharsets.UTF_8.name())
        val message = listOf(
            VERSION,
            state.id,
            encodedName,
            state.seconds.coerceAtLeast(0).toString(),
            state.status,
            state.isRunning.toString(),
            state.updatedAtMillis.toString()
        ).joinToString(FIELD_SEPARATOR)
        return message.toByteArray(StandardCharsets.UTF_8)
    }

    fun decodeRoom(bytes: ByteArray, length: Int): SharedRoomState? {
        return runCatching {
            val message = String(bytes, 0, length, StandardCharsets.UTF_8)
            val fields = message.split(FIELD_SEPARATOR)
            if (fields.size != 7 || fields[0] != VERSION) return null

            SharedRoomState(
                id = fields[1],
                name = URLDecoder.decode(fields[2], StandardCharsets.UTF_8.name()),
                seconds = fields[3].toInt().coerceAtLeast(0),
                status = fields[4],
                isRunning = fields[5].toBooleanStrictOrNull() ?: false,
                updatedAtMillis = fields[6].toLong()
            )
        }.getOrNull()
    }
}
