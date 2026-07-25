package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.BattleRules
import com.github.retro_game.retro_game.battleengine.Combatant
import com.github.retro_game.retro_game.battleengine.CombatantCoordinates
import com.github.retro_game.retro_game.battleengine.UnitAttributes
import com.github.retro_game.retro_game.battleengine.UnitKind
import com.github.retro_game.retro_game.battleengine.UnitKind.*
import java.util.EnumMap

internal object BattleSimQueryParser {
  private const val DEFAULT_ATTACKER_USER_ID = 1L
  private const val DEFAULT_DEFENDER_USER_ID = 2L
  private const val DEFAULT_COORDINATES_KIND_PLANET = 0
  private val defaultAttackerCoordinates = CombatantCoordinates(1, 1, 1, DEFAULT_COORDINATES_KIND_PLANET)

  fun parse(parameters: Map<String, List<String>>): BattleSimInput {
    val attackerCoordinates = optional(parameters, "attacker_pos")
      ?.let { parseCoordinates(it, "attacker_pos") }
      ?: defaultAttackerCoordinates
    val defenderCoordinates = parseCoordinates(required(parameters, "enemy_pos"), "enemy_pos")
    val resources = BattleSimResources(
      metal = optionalLong(parameters, "enemy_metal"),
      crystal = optionalLong(parameters, "enemy_crystal"),
      deuterium = optionalLong(parameters, "enemy_deut"),
    )

    return BattleSimInput(
      resources = resources,
      attacker = parseCombatant(parameters, 'a', DEFAULT_ATTACKER_USER_ID, attackerCoordinates),
      defender = parseCombatant(parameters, 'd', DEFAULT_DEFENDER_USER_ID, defenderCoordinates),
      seed = optionalInt(parameters, "seed", 1),
    )
  }

  private fun parseCombatant(
    parameters: Map<String, List<String>>,
    side: Char,
    userId: Long,
    coordinates: CombatantCoordinates,
  ): Combatant {
    val unitGroups = EnumMap<UnitKind, Long>(UnitKind::class.java)

    for ((index, kind) in BattleSimUnits.unitsByIndex) {
      val count = optionalLong(parameters, "ship_${side}0_${index}_b")
      if (count > 0) {
        if (side == 'a' && kind in BattleSimUnits.defensiveUnitKinds) {
          throw AttackingFleetCannotContainDefensiveUnitsException()
        }
        unitGroups[kind] = count
      }
    }

    return Combatant(
      userId,
      coordinates,
      optionalInt(parameters, "tech_${side}0_0"),
      optionalInt(parameters, "tech_${side}0_1"),
      optionalInt(parameters, "tech_${side}0_2"),
      unitGroups,
    )
  }

  private fun parseCoordinates(value: String, name: String): CombatantCoordinates {
    val parts = value.split(":")
    require(parts.size == 3) { "$name must have format galaxy:system:position" }
    return CombatantCoordinates(
      parsePositiveInt(parts[0], "$name galaxy"),
      parsePositiveInt(parts[1], "$name system"),
      parsePositiveInt(parts[2], "$name position"),
      DEFAULT_COORDINATES_KIND_PLANET,
    )
  }

  private fun required(parameters: Map<String, List<String>>, name: String): String =
    optional(parameters, name) ?: throw IllegalArgumentException("$name is required")

  private fun optionalLong(parameters: Map<String, List<String>>, name: String): Long =
    optional(parameters, name)?.let { parseNonNegativeLong(it, name) } ?: 0L

  private fun optionalInt(parameters: Map<String, List<String>>, name: String, default: Int = 0): Int =
    optional(parameters, name)?.let { parseNonNegativeInt(it, name) } ?: default

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

internal object DefaultBattleSimRules {
  val rules: BattleRules = BattleRules(
    Array(UnitKind.values().size) { index ->
      when (UnitKind.values()[index]) {
        SMALL_CARGO -> attributes(5f, 10f, 4_000f, ESPIONAGE_PROBE to 5, SOLAR_SATELLITE to 5)
        LARGE_CARGO -> attributes(5f, 25f, 12_000f, ESPIONAGE_PROBE to 5, SOLAR_SATELLITE to 5)
        LITTLE_FIGHTER -> attributes(50f, 10f, 4_000f, ESPIONAGE_PROBE to 5, SOLAR_SATELLITE to 5)
        HEAVY_FIGHTER -> attributes(150f, 25f, 10_000f, SMALL_CARGO to 3, ESPIONAGE_PROBE to 5, SOLAR_SATELLITE to 5)
        CRUISER -> attributes(400f, 50f, 27_000f, LITTLE_FIGHTER to 6, ESPIONAGE_PROBE to 5, SOLAR_SATELLITE to 5, ROCKET_LAUNCHER to 10)
        BATTLESHIP -> attributes(1_000f, 200f, 60_000f, ESPIONAGE_PROBE to 5, SOLAR_SATELLITE to 5)
        COLONY_SHIP -> attributes(50f, 100f, 30_000f, ESPIONAGE_PROBE to 5, SOLAR_SATELLITE to 5)
        RECYCLER -> attributes(1f, 10f, 16_000f, ESPIONAGE_PROBE to 5, SOLAR_SATELLITE to 5)
        ESPIONAGE_PROBE -> attributes(0.01f, 0.01f, 1_000f)
        BOMBER -> attributes(1_000f, 500f, 75_000f, ESPIONAGE_PROBE to 5, SOLAR_SATELLITE to 5, ROCKET_LAUNCHER to 20, LIGHT_LASER to 20, HEAVY_LASER to 10, ION_CANNON to 10)
        SOLAR_SATELLITE -> attributes(1f, 1f, 2_000f)
        DESTROYER -> attributes(2_000f, 500f, 110_000f, ESPIONAGE_PROBE to 5, SOLAR_SATELLITE to 5, BATTLE_CRUISER to 2, LIGHT_LASER to 10)
        DEATH_STAR -> attributes(
          200_000f,
          50_000f,
          9_000_000f,
          SMALL_CARGO to 250,
          LARGE_CARGO to 250,
          LITTLE_FIGHTER to 200,
          HEAVY_FIGHTER to 100,
          CRUISER to 33,
          BATTLESHIP to 30,
          COLONY_SHIP to 250,
          RECYCLER to 250,
          ESPIONAGE_PROBE to 1250,
          BOMBER to 25,
          SOLAR_SATELLITE to 1250,
          DESTROYER to 5,
          BATTLE_CRUISER to 15,
          ROCKET_LAUNCHER to 200,
          LIGHT_LASER to 200,
          HEAVY_LASER to 100,
          GAUSS_CANNON to 50,
          ION_CANNON to 100,
        )

        ROCKET_LAUNCHER -> attributes(80f, 20f, 2_000f)
        LIGHT_LASER -> attributes(100f, 25f, 2_000f)
        HEAVY_LASER -> attributes(250f, 100f, 8_000f)
        GAUSS_CANNON -> attributes(1_100f, 200f, 35_000f)
        ION_CANNON -> attributes(150f, 500f, 8_000f)
        PLASMA_TURRET -> attributes(3_000f, 300f, 100_000f)
        SMALL_SHIELD_DOME -> attributes(1f, 2_000f, 20_000f)
        LARGE_SHIELD_DOME -> attributes(1f, 10_000f, 100_000f)
        ANTI_BALLISTIC_MISSILE -> attributes(1f, 1f, 8_000f)
        INTERPLANETARY_MISSILE -> attributes(12_000f, 1f, 15_000f)
        BATTLE_CRUISER -> attributes(700f, 400f, 70_000f, ESPIONAGE_PROBE to 44, SOLAR_SATELLITE to 5, SMALL_CARGO to 3, LARGE_CARGO to 3, HEAVY_FIGHTER to 4, CRUISER to 4, BATTLESHIP to 7)
      }
    },
  )

  private fun attributes(
    weapons: Float,
    shield: Float,
    armor: Float,
    vararg rapidFireAgainst: Pair<UnitKind, Int>,
  ): UnitAttributes = UnitAttributes(weapons, shield, armor, UnitAttributes.makeRapidFire(mapOf(*rapidFireAgainst)))
}
