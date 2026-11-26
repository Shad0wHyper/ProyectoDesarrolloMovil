package com.developers.admin

import com.google.firebase.Timestamp
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AttendanceRecord(
    val id: String = "",
    val key: String = id,
    val userId: String = "",
    val employeeName: String = "",
    val type: String = "ENTRADA",
    val timestamp: Long = 0L,
    val dateFormatted: String = "",
    val timeFormatted: String = ""
)

private val ATTENDANCE_DIACRITICS_REGEX = Regex("\\p{Mn}+")
private val ATTENDANCE_SPACE_REGEX = Regex("\\s+")

internal fun normalizeAttendanceType(rawType: String?, rawTipo: String?): String {
    val raw = listOfNotNull(rawType, rawTipo)
        .firstOrNull { it.isNotBlank() }
        ?: return "ENTRADA"

    val normalized = ATTENDANCE_DIACRITICS_REGEX.replace(
        Normalizer.normalize(raw.trim(), Normalizer.Form.NFD),
        ""
    ).replace(ATTENDANCE_SPACE_REGEX, " ")
        .uppercase(Locale.ROOT)

    return when (normalized) {
        "SALIDA", "SALIDAS", "EXIT", "OUT", "CHECK_OUT", "CHECKOUT",
        "CLOCK_OUT", "CLOCK OUT", "SAIDA", "SAIDAS" -> "SALIDA"
        else -> "ENTRADA"
    }
}

internal fun parseAttendanceTimestampMillis(vararg candidates: Any?): Long {
    for (candidate in candidates) {
        val parsed = when (candidate) {
            is Timestamp -> candidate.toDate().time
            is Number -> candidate.toLong()
            is String -> candidate.trim().toLongOrNull()
            else -> null
        }
        if (parsed != null && parsed > 0L) return parsed
    }
    return 0L
}

internal fun buildAttendanceRecord(
    id: String,
    path: String,
    userId: String,
    rawEmployeeName: String?,
    fallbackEmployeeName: String,
    rawType: String?,
    rawTipo: String?,
    timestamp: Long,
    rawDateFormatted: String?,
    rawTimeFormatted: String?,
    locale: Locale = Locale.getDefault()
): AttendanceRecord {
    val resolvedName = rawEmployeeName?.takeIf { it.isNotBlank() } ?: fallbackEmployeeName
    val resolvedDate = rawDateFormatted?.takeIf { it.isNotBlank() }
        ?: if (timestamp > 0L) SimpleDateFormat("dd MMM yyyy", locale).format(Date(timestamp)) else ""
    val resolvedTime = rawTimeFormatted?.takeIf { it.isNotBlank() }
        ?: if (timestamp > 0L) SimpleDateFormat("hh:mm a", locale).format(Date(timestamp)) else ""

    return AttendanceRecord(
        id = id,
        key = path.ifEmpty { "${userId}_$id" },
        userId = userId,
        employeeName = resolvedName,
        type = normalizeAttendanceType(rawType, rawTipo),
        timestamp = timestamp,
        dateFormatted = resolvedDate,
        timeFormatted = resolvedTime
    )
}

internal fun filterAttendanceRecords(
    records: List<AttendanceRecord>,
    query: String,
    filtroSeleccionado: String
): List<AttendanceRecord> {
    val trimmedQuery = query.trim()
    return records.filter { record ->
        val matchesName = trimmedQuery.isEmpty() || record.employeeName.contains(trimmedQuery, ignoreCase = true)
        val matchesType = filtroSeleccionado == "TODOS" || record.type == filtroSeleccionado
        matchesName && matchesType
    }
}
