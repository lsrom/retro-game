package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.BattleEngineStrategy
import io.javalin.http.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

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

    BattleSimHttpApi(strategy).handle(ctx)

    verify {
      ctx.status(400)
      ctx.contentType("text/plain")
      ctx.result("ERROR: Attacking fleet cannot contain defensive units.")
    }
    verify(exactly = 0) {
      strategy.fight(any(), any(), any(), any())
    }
  }
}
