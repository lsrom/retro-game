package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.BattleOutcome
import com.github.retro_game.retro_game.battleengine.Combatant
import com.github.retro_game.retro_game.battleengine.CombatantOutcome
import com.github.retro_game.retro_game.battleengine.UnitKind
import java.util.EnumMap
import kotlin.math.floor
import kotlin.math.min

internal fun BattleOutcome.toSimOutput(
  input: BattleSimInput,
  universeConfig: UniverseConfig,
  timeTaken: Long
): SimOutput {
  val attackersLossByKind = input.attackers.calcLosses(attackersOutcomes())
  val defendersLossByKind = input.defenders.calcLosses(defendersOutcomes())
  val result = calcBattleResult(input)

  return SimOutput(
    outcome = this,
    result = result,
    debris = calcDebris(attackersLossByKind, defendersLossByKind, universeConfig),
    possiblePlunder = if (result == BattleResult.AttackerWins) {
      input.resources.calcPossiblePlunder(input.attackers.calcRemainingCapacity(attackersOutcomes()))
    } else {
      zeroResources()
    },
    lossesAttacker = attackersLossByKind.totalCost(),
    lossesDefender = defendersLossByKind.totalCost(),
    elapsedTime = timeTaken
  )
}

private fun BattleOutcome.calcBattleResult(input: BattleSimInput): BattleResult {
  val attackersAlive = attackersOutcomes().anyRemainingUnits(numRounds()) { input.attackers.anyUnits() }
  val defendersAlive = defendersOutcomes().anyRemainingUnits(numRounds()) { input.defenders.anyUnits() }
  return when {
    attackersAlive && defendersAlive -> BattleResult.Draw
    attackersAlive -> BattleResult.AttackerWins
    else -> BattleResult.DefenderWins
  }
}

private fun List<Combatant>.calcLosses(outcomes: List<CombatantOutcome>): Map<UnitKind, Long> {
  val losses = EnumMap<UnitKind, Long>(UnitKind::class.java)
  forEachIndexed { index, combatant ->
    val stats = outcomes.getOrNull(index)?.lastRoundStats()
    for (kind in UnitKind.entries) {
      val before = combatant.unitGroups()[kind] ?: 0L
      val after = stats?.get(kind)?.numRemainingUnits() ?: before
      val lost = before - after
      if (lost > 0L) {
        losses[kind] = (losses[kind] ?: 0L) + lost
      }
    }
  }
  return losses
}

private fun List<Combatant>.anyUnits(): Boolean =
  any { combatant -> combatant.unitGroups().values.any { it > 0L } }

private fun List<Combatant>.calcRemainingCapacity(outcomes: List<CombatantOutcome>): Long {
  var capacity = 0L
  forEachIndexed { index, combatant ->
    val stats = outcomes.getOrNull(index)?.lastRoundStats()
    for (kind in UnitKind.entries) {
      val remaining = stats?.get(kind)?.numRemainingUnits() ?: combatant.unitGroups()[kind] ?: 0L
      capacity += remaining * kind.capacity
    }
  }
  return capacity
}

private fun List<CombatantOutcome>.anyRemainingUnits(numRounds: Int, emptyDefault: () -> Boolean): Boolean {
  if (isEmpty() || numRounds == 0) {
    return emptyDefault()
  }
  return any { outcome ->
    val stats = outcome.roundStats(numRounds)
    stats != null && stats.values.any { it.numRemainingUnits() > 0L }
  }
}

private fun CombatantOutcome.roundStats(numRounds: Int) =
  if (numRounds > 0 && unitGroupsStats().isNotEmpty()) getNthRoundUnitGroupsStats(numRounds - 1) else null

private fun CombatantOutcome.lastRoundStats() =
  unitGroupsStats().lastOrNull()

private fun calcDebris(
  attackersLossByKind: Map<UnitKind, Long>,
  defendersLossByKind: Map<UnitKind, Long>,
  universeConfig: UniverseConfig,
): Resources {
  val losses = listOf(attackersLossByKind, defendersLossByKind)
  val metal = losses.sumOf { it.calcDebrisMetal(universeConfig) }
  val crystal = losses.sumOf { it.calcDebrisCrystal(universeConfig) }
  return Resources(metal, crystal, 0L)
}

private fun Map<UnitKind, Long>.calcDebrisMetal(universeConfig: UniverseConfig): Long =
  floor(entries.sumOf { (kind, count) -> kind.cost.metal * count * kind.debrisFactor(universeConfig) }).toLong()

private fun Map<UnitKind, Long>.calcDebrisCrystal(universeConfig: UniverseConfig): Long =
  floor(entries.sumOf { (kind, count) -> kind.cost.crystal * count * kind.debrisFactor(universeConfig) }).toLong()

private fun Map<UnitKind, Long>.totalCost(): Resources =
  Resources(
    metal = entries.sumOf { (kind, count) -> kind.cost.metal * count },
    crystal = entries.sumOf { (kind, count) -> kind.cost.crystal * count },
    deuterium = entries.sumOf { (kind, count) -> kind.cost.deuterium * count },
  )

private fun BattleSimResources.calcPossiblePlunder(capacity: Long): Resources {
  var remainingCapacity = capacity.coerceAtLeast(0L)
  var metalLeft = metal / 2
  var crystalLeft = crystal / 2
  var deuteriumLeft = deuterium / 2
  var plunderMetal = 0L
  var plunderCrystal = 0L
  var plunderDeuterium = 0L

  fun take(available: Long, factor: Long): Long {
    val taken = min(remainingCapacity / factor, available)
    remainingCapacity -= taken
    return taken
  }

  take(metalLeft, 3L).also {
    metalLeft -= it
    plunderMetal += it
  }
  take(crystalLeft, 2L).also {
    crystalLeft -= it
    plunderCrystal += it
  }
  take(deuteriumLeft, 1L).also {
    deuteriumLeft -= it
    plunderDeuterium += it
  }
  take(metalLeft, 2L).also {
    plunderMetal += it
  }
  take(crystalLeft, 2L).also {
    plunderCrystal += it
  }

  return Resources(plunderMetal, plunderCrystal, plunderDeuterium)
}

private fun zeroResources() = Resources(0L, 0L, 0L)

private fun UnitKind.debrisFactor(universeConfig: UniverseConfig): Double =
  if (isFleet) universeConfig.fleetToDebris else universeConfig.defenseToDebris

private val UnitKind.isFleet: Boolean
  get() = when (this) {
    UnitKind.SMALL_CARGO,
    UnitKind.LARGE_CARGO,
    UnitKind.LITTLE_FIGHTER,
    UnitKind.HEAVY_FIGHTER,
    UnitKind.CRUISER,
    UnitKind.BATTLESHIP,
    UnitKind.COLONY_SHIP,
    UnitKind.RECYCLER,
    UnitKind.ESPIONAGE_PROBE,
    UnitKind.BOMBER,
    UnitKind.SOLAR_SATELLITE,
    UnitKind.DESTROYER,
    UnitKind.DEATH_STAR,
    UnitKind.BATTLE_CRUISER -> true

    UnitKind.ROCKET_LAUNCHER,
    UnitKind.LIGHT_LASER,
    UnitKind.HEAVY_LASER,
    UnitKind.GAUSS_CANNON,
    UnitKind.ION_CANNON,
    UnitKind.PLASMA_TURRET,
    UnitKind.SMALL_SHIELD_DOME,
    UnitKind.LARGE_SHIELD_DOME,
    UnitKind.ANTI_BALLISTIC_MISSILE,
    UnitKind.INTERPLANETARY_MISSILE -> false
  }

private val UnitKind.cost: Resources
  get() = when (this) {
    UnitKind.SMALL_CARGO -> Resources(2_000L, 2_000L, 0L)
    UnitKind.LARGE_CARGO -> Resources(6_000L, 6_000L, 0L)
    UnitKind.LITTLE_FIGHTER -> Resources(3_000L, 1_000L, 0L)
    UnitKind.HEAVY_FIGHTER -> Resources(6_000L, 4_000L, 0L)
    UnitKind.CRUISER -> Resources(20_000L, 7_000L, 2_000L)
    UnitKind.BATTLESHIP -> Resources(40_000L, 20_000L, 0L)
    UnitKind.COLONY_SHIP -> Resources(10_000L, 20_000L, 10_000L)
    UnitKind.RECYCLER -> Resources(10_000L, 6_000L, 2_000L)
    UnitKind.ESPIONAGE_PROBE -> Resources(0L, 1_000L, 0L)
    UnitKind.BOMBER -> Resources(50_000L, 25_000L, 15_000L)
    UnitKind.SOLAR_SATELLITE -> Resources(0L, 2_000L, 500L)
    UnitKind.DESTROYER -> Resources(60_000L, 50_000L, 15_000L)
    UnitKind.DEATH_STAR -> Resources(5_000_000L, 4_000_000L, 1_000_000L)
    UnitKind.ROCKET_LAUNCHER -> Resources(2_000L, 0L, 0L)
    UnitKind.LIGHT_LASER -> Resources(1_500L, 500L, 0L)
    UnitKind.HEAVY_LASER -> Resources(6_000L, 2_000L, 0L)
    UnitKind.GAUSS_CANNON -> Resources(20_000L, 15_000L, 2_000L)
    UnitKind.ION_CANNON -> Resources(2_000L, 6_000L, 0L)
    UnitKind.PLASMA_TURRET -> Resources(50_000L, 50_000L, 30_000L)
    UnitKind.SMALL_SHIELD_DOME -> Resources(10_000L, 10_000L, 0L)
    UnitKind.LARGE_SHIELD_DOME -> Resources(50_000L, 50_000L, 0L)
    UnitKind.ANTI_BALLISTIC_MISSILE -> Resources(8_000L, 0L, 2_000L)
    UnitKind.INTERPLANETARY_MISSILE -> Resources(12_500L, 2_500L, 10_000L)
    UnitKind.BATTLE_CRUISER -> Resources(30_000L, 40_000L, 15_000L)
  }

private val UnitKind.capacity: Long
  get() = when (this) {
    UnitKind.SMALL_CARGO -> 5_000L
    UnitKind.LARGE_CARGO -> 25_000L
    UnitKind.LITTLE_FIGHTER -> 50L
    UnitKind.HEAVY_FIGHTER -> 100L
    UnitKind.CRUISER -> 800L
    UnitKind.BATTLESHIP -> 1_500L
    UnitKind.COLONY_SHIP -> 7_500L
    UnitKind.RECYCLER -> 20_000L
    UnitKind.ESPIONAGE_PROBE -> 0L
    UnitKind.BOMBER -> 500L
    UnitKind.SOLAR_SATELLITE -> 0L
    UnitKind.DESTROYER -> 2_000L
    UnitKind.DEATH_STAR -> 1_000_000L
    UnitKind.ROCKET_LAUNCHER -> 0L
    UnitKind.LIGHT_LASER -> 0L
    UnitKind.HEAVY_LASER -> 0L
    UnitKind.GAUSS_CANNON -> 0L
    UnitKind.ION_CANNON -> 0L
    UnitKind.PLASMA_TURRET -> 0L
    UnitKind.SMALL_SHIELD_DOME -> 0L
    UnitKind.LARGE_SHIELD_DOME -> 0L
    UnitKind.ANTI_BALLISTIC_MISSILE -> 0L
    UnitKind.INTERPLANETARY_MISSILE -> 0L
    UnitKind.BATTLE_CRUISER -> 750L
  }
