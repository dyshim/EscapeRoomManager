package com.example.escaperoomshared.network

import com.example.escaperoomshared.model.HintUsageEvent
import com.example.escaperoomshared.model.SharedRoomState
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object TcpProtocol {
    const val PORT = 45991
    private const val VERSION = "1"
    private const val SEP = "|"
    private const val LIST_SEP = ","

    sealed interface Message {
        data class RoomState(val room: SharedRoomState) : Message
        data class RoomCatalog(val activeRoomIds: Set<String>) : Message
        data class HintUsed(val event: HintUsageEvent) : Message
        data class StartRequest(val roomId: String) : Message
        data class StartAccepted(val roomId: String) : Message
        data class RegisterDisplay(val roomId: String) : Message
        data object UnregisterDisplay : Message
        data object Ping : Message
        data object Pong : Message
    }

    fun encodeRoom(state: SharedRoomState): String = listOf(
        VERSION,
        "ROOM",
        encode(state.id),
        encode(state.name),
        state.seconds.coerceAtLeast(0).toString(),
        encode(state.status),
        state.isRunning.toString(),
        state.updatedAtMillis.toString()
    ).joinToString(SEP)

    fun encodeRoomCatalog(roomIds: Collection<String>): String = listOf(
        VERSION,
        "ROOMS",
        roomIds.takeIf { it.isNotEmpty() }?.joinToString(LIST_SEP) { encode(it) } ?: "-"
    ).joinToString(SEP)

    fun encodeHint(event: HintUsageEvent): String = listOf(
        VERSION,
        "HINT",
        encode(event.roomId),
        event.hintNumber.coerceAtLeast(1).toString(),
        event.usedAtMillis.toString()
    ).joinToString(SEP)

    fun encodeStartRequest(roomId: String): String = listOf(VERSION, "START", encode(roomId)).joinToString(SEP)
    fun encodeStartAccepted(roomId: String): String = listOf(VERSION, "STARTED", encode(roomId)).joinToString(SEP)
    fun encodeRegisterDisplay(roomId: String): String = listOf(VERSION, "REGISTER", encode(roomId)).joinToString(SEP)
    fun encodeUnregisterDisplay(): String = "$VERSION${SEP}UNREGISTER"
    fun encodePing(): String = "$VERSION${SEP}PING"
    fun encodePong(): String = "$VERSION${SEP}PONG"

    fun decode(line: String): Message? = runCatching {
        val fields = line.trim().split(SEP)
        if (fields.size < 2 || fields[0] != VERSION) return null

        when (fields[1]) {
            "ROOM" -> {
                if (fields.size != 8) return null
                Message.RoomState(
                    SharedRoomState(
                        id = decodeField(fields[2]),
                        name = decodeField(fields[3]),
                        seconds = fields[4].toInt().coerceAtLeast(0),
                        status = decodeField(fields[5]),
                        isRunning = fields[6].toBooleanStrictOrNull() ?: false,
                        updatedAtMillis = fields[7].toLong()
                    )
                )
            }
            "ROOMS" -> {
                if (fields.size != 3) return null
                val ids = if (fields[2] == "-" || fields[2].isBlank()) emptySet() else fields[2]
                    .split(LIST_SEP)
                    .filter(String::isNotBlank)
                    .map(::decodeField)
                    .toSet()
                Message.RoomCatalog(ids)
            }
            "HINT" -> {
                if (fields.size != 5) return null
                Message.HintUsed(
                    HintUsageEvent(
                        roomId = decodeField(fields[2]),
                        hintNumber = fields[3].toInt().coerceAtLeast(1),
                        usedAtMillis = fields[4].toLong()
                    )
                )
            }
            "START" -> {
                if (fields.size != 3) return null
                Message.StartRequest(decodeField(fields[2]))
            }
            "STARTED" -> {
                if (fields.size != 3) return null
                Message.StartAccepted(decodeField(fields[2]))
            }
            "REGISTER" -> {
                if (fields.size != 3) return null
                Message.RegisterDisplay(decodeField(fields[2]))
            }
            "UNREGISTER" -> Message.UnregisterDisplay
            "PING" -> Message.Ping
            "PONG" -> Message.Pong
            else -> null
        }
    }.getOrNull()

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun decodeField(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
