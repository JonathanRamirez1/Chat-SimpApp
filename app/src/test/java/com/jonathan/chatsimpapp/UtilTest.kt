package com.jonathan.chatsimpapp

import com.jonathan.chatsimpapp.ui.components.initials
import com.jonathan.chatsimpapp.util.ChatIds
import com.jonathan.chatsimpapp.util.TimeFormat
import com.jonathan.chatsimpapp.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class UtilTest {

    @Test
    fun directChatIdIsTheSameWhoeverStartsIt() {
        assertEquals(ChatIds.direct("bob", "alice"), ChatIds.direct("alice", "bob"))
        assertEquals("alice_bob", ChatIds.direct("bob", "alice"))
    }

    @Test
    fun validatesEmails() {
        assertTrue(Validators.isValidEmail("ana@example.com"))
        assertTrue(Validators.isValidEmail("  ana.lopez+chat@mail.co  "))
        assertFalse(Validators.isValidEmail("ana@"))
        assertFalse(Validators.isValidEmail("ana.example.com"))
        assertFalse(Validators.isValidEmail(""))
    }

    @Test
    fun validatesPasswordsAndNames() {
        assertFalse(Validators.isValidPassword("12345"))
        assertTrue(Validators.isValidPassword("123456"))
        assertFalse(Validators.isValidName("   "))
        assertTrue(Validators.isValidName("Ana"))
    }

    @Test
    fun buildsInitials() {
        assertEquals("AL", initials("ana lópez"))
        assertEquals("J", initials("Jonathan"))
        assertEquals("?", initials("  "))
    }

    @Test
    fun classifiesDays() {
        val zone = ZoneId.of("America/Bogota")
        fun at(y: Int, m: Int, d: Int, h: Int) = LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()
        val now = at(2026, 3, 12, 9)
        assertEquals(TimeFormat.Day.TODAY, TimeFormat.dayOf(at(2026, 3, 12, 1), now, zone))
        assertEquals(TimeFormat.Day.YESTERDAY, TimeFormat.dayOf(at(2026, 3, 11, 23), now, zone))
        assertEquals(TimeFormat.Day.THIS_WEEK, TimeFormat.dayOf(at(2026, 3, 8, 12), now, zone))
        assertEquals(TimeFormat.Day.OLDER, TimeFormat.dayOf(at(2026, 2, 1, 12), now, zone))
        assertTrue(TimeFormat.sameDay(at(2026, 3, 12, 1), at(2026, 3, 12, 22), zone))
        assertFalse(TimeFormat.sameDay(at(2026, 3, 12, 1), at(2026, 3, 11, 22), zone))
    }
}
