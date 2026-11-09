package com.jonathan.chatsimpapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Formatos de fecha/hora de la app. Funciones puras (reciben `now`) para poder probarlas. */
object TimeFormat {

    enum class Day { TODAY, YESTERDAY, THIS_WEEK, OLDER }

    fun dayOf(millis: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Day {
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(date, today)
        return when {
            days <= 0L -> Day.TODAY
            days == 1L -> Day.YESTERDAY
            days < 7L -> Day.THIS_WEEK
            else -> Day.OLDER
        }
    }

    fun time(millis: Long, locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .format(Instant.ofEpochMilli(millis).atZone(zone))

    fun shortDate(millis: Long, locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
            .format(Instant.ofEpochMilli(millis).atZone(zone))

    fun longDate(millis: Long, locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): String {
        val date = Instant.ofEpochMilli(millis).atZone(zone)
        val sameYear = date.year == LocalDate.now(zone).year
        val pattern = if (sameYear) "EEEE d MMMM" else "d MMMM yyyy"
        return DateTimeFormatter.ofPattern(pattern, locale).format(date).replaceFirstChar { it.titlecase(locale) }
    }

    fun weekday(millis: Long, locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            .replaceFirstChar { it.titlecase(locale) }

    /** Etiqueta de la lista de chats: "10:30", "Ayer", "Lun" o "12/03/24". */
    fun chatListLabel(millis: Long, yesterday: String, now: Long = System.currentTimeMillis(), locale: Locale = Locale.getDefault()): String =
        when (dayOf(millis, now)) {
            Day.TODAY -> time(millis, locale)
            Day.YESTERDAY -> yesterday
            Day.THIS_WEEK -> weekday(millis, locale)
            Day.OLDER -> shortDate(millis, locale)
        }

    fun sameDay(a: Long, b: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        Instant.ofEpochMilli(a).atZone(zone).toLocalDate() == Instant.ofEpochMilli(b).atZone(zone).toLocalDate()
}
