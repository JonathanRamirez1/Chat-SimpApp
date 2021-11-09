package com.jonathan.chatsimpapp.core.objects

import android.app.Application
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*


object RelativeTime : Application() {

    private const val SECOND_MILLIS = 1000
    private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val DAY_MILLIS = 24 * HOUR_MILLIS

    fun getTimeAgo(time: Long, context: Context?): String? {
        var time = time
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000
        }
        val now = System.currentTimeMillis()
        if (time > now || time <= 0) {
            return "Hace un momento"
        }

        // TODO: localize
        val diff = now - time
        return if (diff < MINUTE_MILLIS) {
            "Hace un momento"
        } else if (diff < 2 * MINUTE_MILLIS) {
            "Hace un minuto"
        } else if (diff < 50 * MINUTE_MILLIS) {
            "Hace " + diff / MINUTE_MILLIS + " minutos"
        } else if (diff < 90 * MINUTE_MILLIS) {
            "Hace una hora"
        } else if (diff < 24 * HOUR_MILLIS) {
            "Hace " + diff / HOUR_MILLIS + " horas"
        } else if (diff < 48 * HOUR_MILLIS) {
            "Ayer"
        } else {
            "Hace " + diff / DAY_MILLIS + " dias"
        }
    }
    fun timeFormatAMPM(time: Long, context: Context?): String? {
            val formatter = SimpleDateFormat("hh:mm a")
            var time = time
            if (time < 1000000000000L) {
                // if timestamp given in seconds, convert to millis
                time *= 1000
            }
            val now = System.currentTimeMillis()
            if (time > now || time <= 0) {
                return formatter.format(Date(time))
            }

            // TODO: localize
            val diff = now - time
            if (diff < 24 * HOUR_MILLIS) {
                return formatter.format(Date(time))
            } else if (diff < 48 * HOUR_MILLIS) {
                return "Ayer"
            } else {
                return "Hace " + diff / DAY_MILLIS + " dias"
            }
        }
    }