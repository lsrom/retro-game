package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.BattleEngineStrategy
import com.github.retro_game.retro_game.battleengine.BattleOutcome
import com.github.retro_game.retro_game.battleengine.CombatantOutcome
import com.github.retro_game.retro_game.battleengine.UnitGroupStats
import com.github.retro_game.retro_game.battleengine.UnitKind
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.EnumMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BattleSimHttpApiTest {

  @Test
  fun `returns plain text error when attacker contains defensive units`() {
    val ctx = mockk<Context>()
    val strategy = mockk<BattleEngineStrategy>()
    every { ctx.queryParamMap() } returns mapOf(
      "enemy_pos" to listOf("1:1:1"),
      "ship_a0_14_b" to listOf("1"),
    )
    every { ctx.status(400) } returns ctx
    every { ctx.contentType("text/plain") } returns ctx
    every { ctx.result("ERROR: Attacking fleet cannot contain defensive units.") } returns ctx

    BattleSimHttpApi(
      strategy = strategy,
      universeConfig = UniverseConfig(),
      engine = "java"
    ).handle(ctx)

    verify {
      ctx.status(400)
      ctx.contentType("text/plain")
      ctx.result("ERROR: Attacking fleet cannot contain defensive units.")
    }
    verify(exactly = 0) {
      strategy.fight(any(), any(), any(), any())
    }
  }

  @Test
  fun `allows empty attacker fleet`() {
    val ctx = mockk<Context>()
    val strategy = mockk<BattleEngineStrategy>()
    every { ctx.queryParamMap() } returns mapOf(
      "enemy_pos" to listOf("1:1:1"),
      "ship_d0_0_b" to listOf("1"),
    )
    every { strategy.fight(emptyList(), any(), any(), any()) } returns BattleOutcome(
      1,
      0,
      emptyList(),
      listOf(combatantOutcome(UnitKind.SMALL_CARGO to 1L)),
    )
    every { ctx.json(any<SimOutput>()) } returns ctx

    BattleSimHttpApi(
      strategy = strategy,
      universeConfig = UniverseConfig(),
      engine = "java"
    ).handle(ctx)

    verify {
      strategy.fight(emptyList(), any(), any(), any())
      ctx.json(any<SimOutput>())
    }
  }

  @Test
  fun `returns error when defender fleet is empty`() {
    val ctx = mockk<Context>()
    val strategy = mockk<BattleEngineStrategy>()
    every { ctx.queryParamMap() } returns mapOf(
      "enemy_pos" to listOf("1:1:1"),
      "ship_a0_0_b" to listOf("1"),
    )

    val error = assertFailsWith<BadRequestResponse> {
      BattleSimHttpApi(
        strategy = strategy,
        universeConfig = UniverseConfig(),
        engine = "java"
      ).handle(ctx)
    }

    assertEquals("Defender fleet cannot be empty.", error.message)
    verify(exactly = 0) {
      strategy.fight(any(), any(), any(), any())
    }
  }

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
