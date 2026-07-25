package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.BattleOutcome
import com.github.retro_game.retro_game.battleengine.Combatant
import com.github.retro_game.retro_game.battleengine.CombatantCoordinates
import com.github.retro_game.retro_game.battleengine.CombatantOutcome
import com.github.retro_game.retro_game.battleengine.UnitGroupStats
import com.github.retro_game.retro_game.battleengine.UnitKind
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.EnumMap
import kotlin.test.Test
import kotlin.test.assertEquals

class BattleExtensionsTest {
  @Test
  fun `builds sim output from battle outcome and sim input`() {
    val input = BattleSimInput(
      resources = BattleSimResources(metal = 9_000L, crystal = 6_000L, deuterium = 3_000L),
      attacker = combatant(1L, UnitKind.SMALL_CARGO to 2L),
      defender = combatant(2L, UnitKind.ROCKET_LAUNCHER to 1L),
    )
    val outcome = BattleOutcome(
      1,
      1,
      listOf(combatantOutcome(UnitKind.SMALL_CARGO to 1L)),
      listOf(combatantOutcome(UnitKind.ROCKET_LAUNCHER to 0L)),
    )

    val universeConfig = UniverseConfig(
      moonshotConfig = MoonshotConfig(debrisPerUnit = 100_000L, maxPercent = 20),
    )

    val output = outcome.toSimOutput(input, universeConfig, 42L)

    assertEquals(outcome, output.outcome)
    assertEquals(BattleResult.AttackerWins, output.result)
    assertEquals(Resources(600L, 600L, 0L), output.debris)
    assertEquals(0.012, output.moonchance)
    assertEquals(Resources(1_749L, 1_709L, 1_500L), output.possiblePlunder)
    assertEquals(Resources(2_000L, 2_000L, 0L), output.lossesAttacker)
    assertEquals(Resources(2_000L, 0L, 0L), output.lossesDefender)
    assertEquals(42L, output.elapsedTime)
  }

  @ParameterizedTest
  @CsvSource(
    "1",
    "2",
    "3",
    "5",
    "10",
    "20",
    "30",
    "50",
    "65",
    "80",
    "90",
    "100",
  )
  fun `calculates moonchance from debris using moonshot config`(expectedPercent: Long) {
    val input = BattleSimInput(
      resources = BattleSimResources(metal = 0L, crystal = 0L, deuterium = 0L),
      attacker = combatant(1L, UnitKind.ESPIONAGE_PROBE to expectedPercent),
      defender = combatant(2L, UnitKind.ROCKET_LAUNCHER to 1L),
    )
    val outcome = BattleOutcome(
      1,
      1,
      listOf(combatantOutcome(UnitKind.ESPIONAGE_PROBE to 0L)),
      listOf(combatantOutcome(UnitKind.ROCKET_LAUNCHER to 1L)),
    )
    val universeConfig = UniverseConfig(
      fleetToDebris = 1.0,
      moonshotConfig = MoonshotConfig(debrisPerUnit = 1_000L, maxPercent = 100),
    )

    val output = outcome.toSimOutput(input, universeConfig, 42L)

    assertEquals(Resources(0L, expectedPercent * 1_000L, 0L), output.debris)
    assertEquals(expectedPercent.toDouble(), output.moonchance)
  }

  @Test
  fun `caps moonchance at configured max percent`() {
    val input = BattleSimInput(
      resources = BattleSimResources(metal = 0L, crystal = 0L, deuterium = 0L),
      attacker = combatant(1L, UnitKind.LITTLE_FIGHTER to 50L),
      defender = combatant(2L, UnitKind.LITTLE_FIGHTER to 1L),
    )
    val outcome = BattleOutcome(
      1,
      1,
      listOf(combatantOutcome(UnitKind.LITTLE_FIGHTER to 0L)),
      listOf(combatantOutcome(UnitKind.LITTLE_FIGHTER to 0L)),
    )
    val universeConfig = UniverseConfig(
      fleetToDebris = 1.0,
      moonshotConfig = MoonshotConfig(debrisPerUnit = 10_000L, maxPercent = 5),
    )

    val output = outcome.toSimOutput(input, universeConfig, 42L)

    assertEquals(5.0, output.moonchance)
  }

  private fun combatant(userId: Long, vararg unitGroups: Pair<UnitKind, Long>): Combatant =
    Combatant(
      userId,
      CombatantCoordinates(1, 1, userId.toInt(), 0),
      0,
      0,
      0,
      EnumMap<UnitKind, Long>(UnitKind::class.java).also { groups ->
        for ((kind, count) in unitGroups) {
          groups[kind] = count
        }
      },
    )

  private fun combatantOutcome(vararg remainingUnits: Pair<UnitKind, Long>): CombatantOutcome =
    CombatantOutcome(
      listOf(
        EnumMap<UnitKind, UnitGroupStats>(UnitKind::class.java).also { stats ->
          for (kind in UnitKind.entries) {
            val count = remainingUnits.firstOrNull { it.first == kind }?.second ?: 0L
            stats[kind] = UnitGroupStats(count, 0L, 0L, 0f, 0f, 0f, 0f)
          }
        },
      ),
    )
}
