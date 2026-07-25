package com.github.retro_game.retro_game.battleengineservice

data class UniverseConfig(
    val fleetToDebris: Double = 0.3,
    val defenseToDebris: Double = 0.0,
    val moonshotConfig: MoonshotConfig = MoonshotConfig(
        debrisPerUnit = 100_000,
        maxPercent = 20
    ),
    val useNativeCombatEngine: Boolean = false
)

data class MoonshotConfig(
    val debrisPerUnit: Long,
    val maxPercent: Int
)