package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.BattleEngineStrategy
import com.github.retro_game.retro_game.battleengine.BattleRules
import com.github.retro_game.retro_game.battleengine.Combatant
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import java.util.UUID

class BattleSimHttpApi(
  private val strategy: BattleEngineStrategy,
  private val universeConfig: UniverseConfig,
  private val historyDatabase: HistoryDatabase? = null,
) : Handler {

  override fun handle(ctx: Context) {
    try {
      val input = BattleSimQueryParser.parse(ctx.queryParamMap())
      input.requireNonEmptyFleets()
      val time = System.currentTimeMillis()
      val outcome = strategy.fight(input.attackers, input.defenders, input.rules, input.seed)
      val fightDuration = System.currentTimeMillis() - time
      val output = outcome.toSimOutput(input, universeConfig, fightDuration)

      historyDatabase?.save(output.toHistoryItem(input, ctx.queryString() ?: ""))

      ctx.json(output)
    } catch (e: AttackingFleetCannotContainDefensiveUnitsException) {
      ctx.status(400)
        .contentType("text/plain")
        .result("ERROR: ${e.message}")
    } catch (e: IllegalArgumentException) {
      throw BadRequestResponse(e.message ?: "Invalid simulation query")
    }
  }
}

private fun BattleSimInput.requireNonEmptyFleets() {
  require(attackers.isNotEmpty()) { "Attacker fleet cannot be empty." }
  require(defenders.isNotEmpty()) { "Defender fleet cannot be empty." }
}

private fun SimOutput.toHistoryItem(input: BattleSimInput, query: String): HistoryItem =
  HistoryItem(
    id = UUID.randomUUID(),
    utcTimestamp = System.currentTimeMillis(),
    query = query,
    seed = input.seed.toLong(),
    totalAttackerLosses = lossesAttacker.total(),
    totalDefenderLosses = lossesDefender.total(),
    totalDebrisField = debris.total(),
    plunder = possiblePlunder.total(),
    elapsedTime = elapsedTime,
  )

private fun Resources.total(): Long = metal + crystal + deuterium

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
