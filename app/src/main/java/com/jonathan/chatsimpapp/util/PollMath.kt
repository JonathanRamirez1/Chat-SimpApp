package com.jonathan.chatsimpapp.util

import com.jonathan.chatsimpapp.data.model.Vote
import kotlin.math.ceil

/** Resultados de una votación: un voto por unidad y quórum sobre el total de unidades. */
data class PollResult(
    val counts: List<Int>,
    val totalVotes: Int,
    val unitsCount: Int,
    val quorumPercent: Int,
) {
    /** Unidades necesarias para que la votación sea válida. */
    val quorumNeeded: Int get() = if (unitsCount <= 0) 0 else ceil(unitsCount * quorumPercent / 100.0).toInt()
    val quorumReached: Boolean get() = totalVotes >= quorumNeeded && totalVotes > 0
    val participation: Float get() = if (unitsCount <= 0) 0f else (totalVotes.toFloat() / unitsCount).coerceAtMost(1f)

    fun share(option: Int): Float = if (totalVotes == 0) 0f else counts[option].toFloat() / totalVotes

    /** Índice de la opción ganadora, o null si hay empate o no hay votos. */
    val winner: Int?
        get() {
            val max = counts.maxOrNull() ?: return null
            if (max == 0) return null
            return counts.indexOf(max).takeIf { counts.count { it == max } == 1 }
        }

    companion object {
        fun of(votes: List<Vote>, optionCount: Int, unitsCount: Int, quorumPercent: Int): PollResult {
            val counts = MutableList(optionCount) { 0 }
            votes.forEach { v -> v.option.toInt().takeIf { it in 0 until optionCount }?.let { counts[it]++ } }
            return PollResult(counts, counts.sum(), unitsCount, quorumPercent)
        }
    }
}
