package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.UnitKind
import com.github.retro_game.retro_game.battleengine.UnitKind.*

internal object BattleSimUnits {
  val unitsByIndex: Map<Int, UnitKind> = linkedMapOf(
    0 to SMALL_CARGO,
    1 to LARGE_CARGO,
    2 to LITTLE_FIGHTER,
    3 to HEAVY_FIGHTER,
    4 to CRUISER,
    5 to BATTLESHIP,
    6 to COLONY_SHIP,
    7 to RECYCLER,
    8 to ESPIONAGE_PROBE,
    9 to BOMBER,
    10 to SOLAR_SATELLITE,
    11 to DESTROYER,
    12 to DEATH_STAR,
    13 to BATTLE_CRUISER,
    14 to ROCKET_LAUNCHER,
    15 to LIGHT_LASER,
    16 to HEAVY_LASER,
    17 to GAUSS_CANNON,
    18 to ION_CANNON,
    19 to PLASMA_TURRET,
    20 to SMALL_SHIELD_DOME,
    21 to LARGE_SHIELD_DOME,
  )

  val metadata: List<BattleSimUnitMetadata> = unitsByIndex.map { (index, kind) ->
    BattleSimUnitMetadata(index, kind.name, kind.displayName())
  }

  private fun UnitKind.displayName(): String =
    name.lowercase()
      .split('_')
      .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}

data class BattleSimUnitMetadata(
  val index: Int,
  val kind: String,
  val name: String,
)
