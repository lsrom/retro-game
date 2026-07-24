package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.Combatant
import com.github.retro_game.retro_game.battleengine.CombatantCoordinates
import com.github.retro_game.retro_game.battleengine.UnitKind
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import java.util.EnumMap

class BattleSimHttpApi : Handler {
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


