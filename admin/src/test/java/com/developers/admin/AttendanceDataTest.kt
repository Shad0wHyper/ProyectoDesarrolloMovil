package com.developers.admin

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.Locale

class AttendanceDataTest {

    @Test
    fun `normalizeAttendanceType handles entry and exit variants`() {
        assertEquals("ENTRADA", normalizeAttendanceType("ENTRADA", null))
        assertEquals("ENTRADA", normalizeAttendanceType("entrada", null))
        assertEquals("ENTRADA", normalizeAttendanceType(null, "Entradas"))
        assertEquals("ENTRADA", normalizeAttendanceType("check_in", null))
        assertEquals("ENTRADA", normalizeAttendanceType(null, null))

        assertEquals("SALIDA", normalizeAttendanceType("SALIDA", null))
        assertEquals("SALIDA", normalizeAttendanceType("salida", null))
        assertEquals("SALIDA", normalizeAttendanceType(null, "Salidas"))
        assertEquals("SALIDA", normalizeAttendanceType("check_out", null))
        assertEquals("SALIDA", normalizeAttendanceType("Saída", null))
    }

    @Test
    fun `parseAttendanceTimestampMillis handles Timestamp, Number, and String`() {
        val millis = 1_764_100_000_000L
        assertEquals(millis, parseAttendanceTimestampMillis(Timestamp(Date(millis))))
        assertEquals(millis, parseAttendanceTimestampMillis(null, millis))
        assertEquals(millis, parseAttendanceTimestampMillis(null, null, millis.toString()))
        assertEquals(0L, parseAttendanceTimestampMillis(null, 0L, "bad"))
    }

    @Test
    fun `buildAttendanceRecord fills missing formatted date and time from timestamp`() {
        val millis = 1_764_100_000_000L
        val record = buildAttendanceRecord(
            id = "log1",
            path = "usuarios/u1/asistencias/log1",
            userId = "u1",
            rawEmployeeName = "",
            fallbackEmployeeName = "Empleado",
            rawType = null,
            rawTipo = "salida",
            timestamp = millis,
            rawDateFormatted = "",
            rawTimeFormatted = "",
            locale = Locale.US
        )
        assertEquals("usuarios/u1/asistencias/log1", record.key)
        assertEquals("Empleado", record.employeeName)
        assertEquals("SALIDA", record.type)
        assertEquals(true, record.dateFormatted.isNotBlank())
        assertEquals(true, record.timeFormatted.isNotBlank())
    }

    @Test
    fun `filterAttendanceRecords filters by type and employee name`() {
        val records = listOf(
            AttendanceRecord(id = "1", key = "k1", userId = "u1", employeeName = "Ana López", type = "ENTRADA", timestamp = 3L),
            AttendanceRecord(id = "2", key = "k2", userId = "u1", employeeName = "Ana López", type = "SALIDA", timestamp = 2L),
            AttendanceRecord(id = "3", key = "k3", userId = "u2", employeeName = "Carlos Ruiz", type = "ENTRADA", timestamp = 1L)
        )

        assertEquals(3, filterAttendanceRecords(records, "", "TODOS").size)
        assertEquals(2, filterAttendanceRecords(records, "", "ENTRADA").size)
        assertEquals(1, filterAttendanceRecords(records, "", "SALIDA").size)
        assertEquals(1, filterAttendanceRecords(records, "ana", "ENTRADA").size)
        assertEquals(1, filterAttendanceRecords(records, " carlos ", "TODOS").size)
    }
}
