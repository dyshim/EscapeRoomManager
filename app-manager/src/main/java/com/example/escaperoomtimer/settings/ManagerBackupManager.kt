package com.example.escaperoomtimer.settings

import android.content.Context
import android.net.Uri
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.model.ThemePreset
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupHistoryItem(
    val fileName: String,
    val createdAtEpochMillis: Long,
    val roomCount: Int,
    val presetCount: Int
)

data class ManagerBackup(
    val createdAtEpochMillis: Long,
    val storeInfo: StoreInfo,
    val rooms: List<RoomInfo>,
    val presets: List<ThemePreset>,
    val alarmSettings: ManagerAlarmSettings
)

object ManagerBackupManager {
    const val FILE_EXTENSION = "ers"
    private const val FORMAT = "EscapeRoomSuiteManagerBackup"
    private const val VERSION = 1
    private const val BACKUP_DIRECTORY = "manager_backups"
    private const val MAX_HISTORY = 3

    fun write(context: Context, uri: Uri, backup: ManagerBackup) {
        val json = encode(backup).toString(2)
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt"))
            .bufferedWriter(Charsets.UTF_8)
            .use { it.write(json) }
    }

    fun read(context: Context, uri: Uri): ManagerBackup {
        val raw = requireNotNull(context.contentResolver.openInputStream(uri))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return decode(JSONObject(raw))
    }

    fun saveInternal(context: Context, fileName: String, backup: ManagerBackup) {
        val directory = File(context.filesDir, BACKUP_DIRECTORY).apply { mkdirs() }
        File(directory, fileName.safeFileName()).writeText(encode(backup).toString(2), Charsets.UTF_8)
        directory.listFiles()?.sortedByDescending(File::lastModified)?.drop(MAX_HISTORY)?.forEach(File::delete)
    }

    fun history(context: Context): List<BackupHistoryItem> {
        val directory = File(context.filesDir, BACKUP_DIRECTORY)
        return directory.listFiles()
            ?.sortedByDescending(File::lastModified)
            ?.take(MAX_HISTORY)
            ?.mapNotNull { file ->
                runCatching { decode(JSONObject(file.readText(Charsets.UTF_8))) }.getOrNull()?.let {
                    BackupHistoryItem(file.name, it.createdAtEpochMillis, it.rooms.size, it.presets.size)
                }
            }
            .orEmpty()
    }

    fun readInternal(context: Context, fileName: String): ManagerBackup {
        val file = File(File(context.filesDir, BACKUP_DIRECTORY), fileName.safeFileName())
        require(file.isFile) { "백업 파일을 찾을 수 없습니다." }
        return decode(JSONObject(file.readText(Charsets.UTF_8)))
    }

    fun createSafetyBackup(context: Context, backup: ManagerBackup) {
        saveInternal(context, "Safety_${backup.createdAtEpochMillis}.$FILE_EXTENSION", backup)
    }

    private fun String.safeFileName(): String = substringAfterLast('/').substringAfterLast('\\')
        .takeIf { it.endsWith(".$FILE_EXTENSION", ignoreCase = true) }
        ?: "Backup_${System.currentTimeMillis()}.$FILE_EXTENSION"

    private fun encode(backup: ManagerBackup) = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("createdAtEpochMillis", backup.createdAtEpochMillis)
        put("storeInfo", JSONObject().apply {
            put("storeName", backup.storeInfo.storeName)
            put("branchName", backup.storeInfo.branchName)
        })
        put("rooms", JSONArray().apply {
            backup.rooms.forEach { room ->
                put(JSONObject().apply {
                    put("id", room.id)
                    put("name", room.name)
                    put("defaultMinutes", room.defaultMinutes)
                    put("hintEnabled", room.hintEnabled)
                    put("guestScreenEnabled", room.guestScreenEnabled)
                    put("isEnabled", room.isEnabled)
                    put("isMaintenance", room.isMaintenance)
                })
            }
        })
        put("presets", JSONArray().apply {
            backup.presets.forEach { preset ->
                put(JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("defaultMinutes", preset.defaultMinutes)
                    put("emoji", preset.emoji)
                })
            }
        })
        put("alarm", JSONObject().apply {
            put("soundUri", backup.alarmSettings.soundUri ?: JSONObject.NULL)
            put("autoStopSeconds", backup.alarmSettings.autoStopSeconds)
            put("vibrationEnabled", backup.alarmSettings.vibrationEnabled)
        })
    }

    private fun decode(root: JSONObject): ManagerBackup {
        require(root.optString("format") == FORMAT) { "지원하지 않는 백업 파일입니다." }
        require(root.optInt("version") == VERSION) { "지원하지 않는 백업 버전입니다." }

        val store = root.getJSONObject("storeInfo")
        val roomsArray = root.getJSONArray("rooms")
        val rooms = buildList {
            for (index in 0 until roomsArray.length()) {
                val item = roomsArray.getJSONObject(index)
                val id = item.getString("id").trim()
                val name = item.getString("name").trim()
                require(id.isNotBlank() && name.isNotBlank()) { "잘못된 테마 정보가 있습니다." }
                val minutes = item.optInt("defaultMinutes", 60).coerceIn(1, 240)
                add(RoomInfo(
                    id = id,
                    name = name,
                    seconds = minutes * 60,
                    status = RoomStatus.WAITING,
                    defaultMinutes = minutes,
                    hintEnabled = item.optBoolean("hintEnabled", true),
                    guestScreenEnabled = item.optBoolean("guestScreenEnabled", true),
                    isEnabled = item.optBoolean("isEnabled", true),
                    isMaintenance = item.optBoolean("isMaintenance", false)
                ))
            }
        }
        require(rooms.isNotEmpty()) { "백업에 테마가 없습니다." }
        require(rooms.map { it.id }.distinct().size == rooms.size) { "중복된 테마가 있습니다." }

        val presetsArray = root.optJSONArray("presets") ?: JSONArray()
        val presets = buildList {
            for (index in 0 until presetsArray.length()) {
                val item = presetsArray.getJSONObject(index)
                val name = item.optString("name").trim()
                if (name.isNotBlank()) add(ThemePreset(
                    id = item.optString("id", "preset_$index"),
                    name = name,
                    defaultMinutes = item.optInt("defaultMinutes", 60).coerceIn(1, 240),
                    emoji = item.optString("emoji", "🎭").ifBlank { "🎭" }
                ))
            }
        }
        val alarm = root.optJSONObject("alarm") ?: JSONObject()
        return ManagerBackup(
            createdAtEpochMillis = root.optLong("createdAtEpochMillis", 0L),
            storeInfo = StoreInfo(
                storeName = store.optString("storeName").trim().take(30),
                branchName = store.optString("branchName").trim().take(20)
            ),
            rooms = rooms,
            presets = presets,
            alarmSettings = ManagerAlarmSettings(
                soundUri = if (alarm.isNull("soundUri")) null else alarm.optString("soundUri").ifBlank { null },
                autoStopSeconds = alarm.optInt("autoStopSeconds", 30).coerceIn(0, 600),
                vibrationEnabled = alarm.optBoolean("vibrationEnabled", true)
            )
        )
    }
}
