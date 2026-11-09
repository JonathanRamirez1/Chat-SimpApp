package com.jonathan.chatsimpapp

import com.jonathan.chatsimpapp.ai.SummaryGenerator
import com.jonathan.chatsimpapp.data.model.Announcement
import com.jonathan.chatsimpapp.data.model.Community
import com.jonathan.chatsimpapp.data.model.Message
import com.jonathan.chatsimpapp.data.model.Vote
import com.google.firebase.Timestamp
import com.jonathan.chatsimpapp.util.InviteCodes
import com.jonathan.chatsimpapp.util.PollResult
import com.jonathan.chatsimpapp.util.Weeks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Date

class CommunityLogicTest {

    private fun votes(vararg options: Int) = options.mapIndexed { i, o -> Vote(unit = "${100 + i}", option = o.toLong()) }

    @Test
    fun pollQuorumIsCountedOverAllUnits() {
        val r = PollResult.of(votes(0, 0, 1), optionCount = 2, unitsCount = 10, quorumPercent = 50)
        assertEquals(listOf(2, 1), r.counts)
        assertEquals(5, r.quorumNeeded)
        assertFalse(r.quorumReached)
        assertEquals(0, r.winner)
        assertEquals(0.3f, r.participation, 0.001f)

        val ok = PollResult.of(votes(0, 1, 1, 1, 0), optionCount = 2, unitsCount = 10, quorumPercent = 50)
        assertTrue(ok.quorumReached)
        assertEquals(1, ok.winner)
    }

    @Test
    fun pollTieHasNoWinnerAndQuorumRoundsUp() {
        val r = PollResult.of(votes(0, 1), optionCount = 2, unitsCount = 7, quorumPercent = 67)
        assertNull(r.winner)
        assertEquals(5, r.quorumNeeded) // 7 * 0.67 = 4.69 -> 5 unidades
    }

    @Test
    fun pollIgnoresInvalidOptions() {
        val r = PollResult.of(votes(0, 7, -1), optionCount = 2, unitsCount = 3, quorumPercent = 0)
        assertEquals(1, r.totalVotes)
    }

    @Test
    fun inviteCodesAreReadableAndNormalized() {
        repeat(50) {
            val code = InviteCodes.generate()
            assertTrue(InviteCodes.isValid(code))
            assertFalse(code.any { it in "01OIL" })
        }
        assertEquals("ABC234", InviteCodes.normalize(" abc-234 "))
        assertFalse(InviteCodes.isValid("ABC10"))
    }

    @Test
    fun isoWeekKey() {
        assertEquals("2026-W46", Weeks.key(LocalDate.of(2026, 11, 9)))
        assertEquals("2026-W01", Weeks.key(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun proPlanExpires() {
        val future = Community(plan = Community.PLAN_PRO, planExpiresAt = Timestamp(Date(System.currentTimeMillis() + 60_000)))
        val past = future.copy(planExpiresAt = Timestamp(Date(System.currentTimeMillis() - 60_000)))
        assertTrue(future.isPro)
        assertFalse(past.isPro)
        assertFalse(Community().isPro)
    }

    @Test
    fun summaryPromptUsesUnitsNotNames() {
        val prompt = SummaryGenerator.buildPrompt(
            communityName = "Torres del Parque",
            messages = listOf(Message(senderId = "u1", text = "Yo llamo al plomero mañana")),
            announcements = listOf(Announcement(title = "Corte de agua", body = "Jueves 8am")),
            reports = emptyList(),
            units = mapOf("u1" to "502"),
        )
        assertTrue(prompt.contains("Apto 502: Yo llamo al plomero mañana"))
        assertTrue(prompt.contains("Corte de agua"))
        assertFalse(prompt.contains("u1"))
    }
}
