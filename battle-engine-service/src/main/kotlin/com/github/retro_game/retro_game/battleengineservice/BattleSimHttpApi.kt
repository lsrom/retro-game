package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.BattleEngineStrategy
import com.github.retro_game.retro_game.battleengine.Combatant
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler

class BattleSimHttpApi(
  private val strategy: BattleEngineStrategy
) : Handler {
  override fun handle(ctx: Context) {
    try {
      ctx.json(BattleSimQueryParser.parse(ctx.queryParamMap()))
    } catch (e: IllegalArgumentException) {
      throw BadRequestResponse(e.message ?: "Invalid simulation query")
    }
  }
}

data class BattleSimInput(
  val resources: BattleSimResources,
  val defender: Combatant,
)

data class BattleSimResources(
  val metal: Long,
  val crystal: Long,
  val deuterium: Long,
)


