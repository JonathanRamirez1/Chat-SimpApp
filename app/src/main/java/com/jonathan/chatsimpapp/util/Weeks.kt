package com.jonathan.chatsimpapp.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

object Weeks {
    /** Clave de la semana ISO, p. ej. "2026-W45". */
    fun key(date: LocalDate = LocalDate.now()): String =
        "%d-W%02d".format(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))

    /** Inicio (lunes 00:00) de la semana, en milisegundos. */
    fun startMillis(date: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Long =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant().toEpochMilli()

    fun lastSevenDaysStart(now: Long = System.currentTimeMillis()): Long =
        Instant.ofEpochMilli(now).minusSeconds(7 * 24 * 3600).toEpochMilli()
}
