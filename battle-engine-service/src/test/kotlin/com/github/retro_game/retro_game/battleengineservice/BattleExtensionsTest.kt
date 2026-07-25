package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.BattleOutcome
import com.github.retro_game.retro_game.battleengine.Combatant
import com.github.retro_game.retro_game.battleengine.CombatantCoordinates
import com.github.retro_game.retro_game.battleengine.CombatantOutcome
import com.github.retro_game.retro_game.battleengine.UnitGroupStats
import com.github.retro_game.retro_game.battleengine.UnitKind
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

    val output = outcome.toSimOutput(input, UniverseConfig())

    assertEquals(outcome, output.outcome)
    assertEquals(BattleResult.AttackerWins, output.result)
    assertEquals(Resources(600L, 600L, 0L), output.debris)
    assertEquals(Resources(1_749L, 1_709L, 1_500L), output.possiblePlunder)
    assertEquals(Resources(2_000L, 2_000L, 0L), output.lossesAttacker)
    assertEquals(Resources(2_000L, 0L, 0L), output.lossesDefender)
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
