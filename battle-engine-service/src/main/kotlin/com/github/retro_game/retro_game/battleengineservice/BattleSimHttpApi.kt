package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.BattleEngineStrategy
import com.github.retro_game.retro_game.battleengine.BattleRules
import com.github.retro_game.retro_game.battleengine.Combatant
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler

class BattleSimHttpApi(
  private val strategy: BattleEngineStrategy,
  private val universeConfig: UniverseConfig = UniverseConfig(),
) : Handler {
  override fun handle(ctx: Context) {
    try {
      val input = BattleSimQueryParser.parse(ctx.queryParamMap())
      val outcome = strategy.fight(input.attackers, input.defenders, input.rules, input.seed)
      ctx.json(outcome.toSimOutput(input, universeConfig))
    } catch (e: AttackingFleetCannotContainDefensiveUnitsException) {
      ctx.status(400)
        .contentType("text/plain")
        .result("ERROR: ${e.message}")
    } catch (e: IllegalArgumentException) {
      throw BadRequestResponse(e.message ?: "Invalid simulation query")
    }
  }
}

data class BattleSimInput(
  val resources: BattleSimResources,
  val attacker: Combatant,
  val defender: Combatant,
  val rules: BattleRules = DefaultBattleSimRules.rules,
  val seed: Int = 1,
) {
  val attackers: List<Combatant> = if (attacker.unitGroups().isEmpty()) emptyList() else listOf(attacker)
  val defenders: List<Combatant> = if (defender.unitGroups().isEmpty()) emptyList() else listOf(defender)
}

data class BattleSimResources(
  val metal: Long,
  val crystal: Long,
  val deuterium: Long,
)
