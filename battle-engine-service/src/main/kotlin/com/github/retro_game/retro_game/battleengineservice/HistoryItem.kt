package com.github.retro_game.retro_game.battleengineservice

import java.util.UUID

data class HistoryItem(
    val id: UUID,
    val utcTimestamp: Long,
    val query: String,
    val seed: Long,
    val totalAttackerLosses: Long,
    val totalDefenderLosses: Long,
    val totalDebrisField: Long,
    val plunder: Long,
    val elapsedTime: Long
)
