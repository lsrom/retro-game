package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.BattleOutcome

data class SimOutput(
    val outcome: BattleOutcome,
    val result: BattleResult,
    val debris: Resources,
    val moonchance: Double,
    val possiblePlunder: Resources,
    val lossesAttacker: Resources,
    val lossesDefender: Resources,
    val elapsedTime: Long
)

enum class BattleResult {
    AttackerWins, DefenderWins, Draw
}

data class Resources(
    val metal: Long,
    val crystal: Long,
    val deuterium: Long
)
