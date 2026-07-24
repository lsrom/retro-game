package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.Combatant
import com.github.retro_game.retro_game.battleengine.CombatantCoordinates
import com.github.retro_game.retro_game.battleengine.UnitKind
import com.github.retro_game.retro_game.battleengine.UnitKind.*
import java.util.EnumMap

internal object BattleSimQueryParser {

  private const val DEFAULT_DEFENDER_USER_ID = 0L
  private const val DEFAULT_COORDINATES_KIND_PLANET = 0

  private val unitsByParameter = mapOf(
    "ship_d0_0_b" to SMALL_CARGO,
    "ship_d0_1_b" to LARGE_CARGO,
    "ship_d0_2_b" to LITTLE_FIGHTER,
    "ship_d0_3_b" to HEAVY_FIGHTER,
    "ship_d0_4_b" to CRUISER,
    "ship_d0_5_b" to BATTLESHIP,
    "ship_d0_6_b" to COLONY_SHIP,
    "ship_d0_7_b" to RECYCLER,
    "ship_d0_8_b" to ESPIONAGE_PROBE,
    "ship_d0_9_b" to BOMBER,
    "ship_d0_10_b" to SOLAR_SATELLITE,
    "ship_d0_11_b" to DESTROYER,
    "ship_d0_12_b" to DEATH_STAR,
    "ship_d0_13_b" to BATTLE_CRUISER,
    "ship_d0_14_b" to ROCKET_LAUNCHER,
    "ship_d0_15_b" to LIGHT_LASER,
    "ship_d0_16_b" to HEAVY_LASER,
    "ship_d0_17_b" to GAUSS_CANNON,
    "ship_d0_18_b" to ION_CANNON,
    "ship_d0_19_b" to PLASMA_TURRET,
    "ship_d0_20_b" to SMALL_SHIELD_DOME,
    "ship_d0_21_b" to LARGE_SHIELD_DOME,
  )

  fun parse(parameters: Map<String, List<String>>): BattleSimInput {
    val coordinates = parseCoordinates(required(parameters, "enemy_pos"))
    val resources = BattleSimResources(
      metal = optionalLong(parameters, "enemy_metal"),
      crystal = optionalLong(parameters, "enemy_crystal"),
      deuterium = optionalLong(parameters, "enemy_deut"),
    )
    val unitGroups = EnumMap<UnitKind, Long>(UnitKind::class.java)

    for ((parameter, kind) in unitsByParameter) {
      val count = optionalLong(parameters, parameter)
      if (count > 0) {
        unitGroups.merge(kind, count, Long::plus)
      }
    }

    val defender = Combatant(
      DEFAULT_DEFENDER_USER_ID,
      coordinates,
      optionalInt(parameters, "tech_d0_0"),
      optionalInt(parameters, "tech_d0_1"),
      optionalInt(parameters, "tech_d0_2"),
      unitGroups,
    )
    return BattleSimInput(resources, defender)
  }

  private fun parseCoordinates(value: String): CombatantCoordinates {
    val parts = value.split(":")
    require(parts.size == 3) { "enemy_pos must have format galaxy:system:position" }
    return CombatantCoordinates(
      parsePositiveInt(parts[0], "enemy_pos galaxy"),
      parsePositiveInt(parts[1], "enemy_pos system"),
      parsePositiveInt(parts[2], "enemy_pos position"),
      DEFAULT_COORDINATES_KIND_PLANET,
    )
  }

  private fun required(parameters: Map<String, List<String>>, name: String): String =
    optional(parameters, name) ?: throw IllegalArgumentException("$name is required")

  private fun optionalLong(parameters: Map<String, List<String>>, name: String): Long =
    optional(parameters, name)?.let { parseNonNegativeLong(it, name) } ?: 0L

  private fun optionalInt(parameters: Map<String, List<String>>, name: String): Int =
    optional(parameters, name)?.let { parseNonNegativeInt(it, name) } ?: 0

  private fun optional(parameters: Map<String, List<String>>, name: String): String? {
    val values = parameters[name] ?: return null
    require(values.size == 1) { "$name must be provided at most once" }
    return values.single()
  }

  private fun parsePositiveInt(value: String, name: String): Int {
    val parsed = value.toIntOrNull() ?: throw IllegalArgumentException("$name must be an integer")
    require(parsed > 0) { "$name must be positive" }
    return parsed
  }

  private fun parseNonNegativeInt(value: String, name: String): Int {
    val parsed = value.toIntOrNull() ?: throw IllegalArgumentException("$name must be an integer")
    require(parsed >= 0) { "$name must be non-negative" }
    return parsed
  }

  private fun parseNonNegativeLong(value: String, name: String): Long {
    val parsed = value.toLongOrNull() ?: throw IllegalArgumentException("$name must be an integer")
    require(parsed >= 0L) { "$name must be non-negative" }
    return parsed
  }
}