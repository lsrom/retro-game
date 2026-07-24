package com.github.retro_game.retro_game.battleengineservice

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.retro_game.retro_game.battleengine.BattleOutcome
import com.github.retro_game.retro_game.battleengine.BattleRules
import com.github.retro_game.retro_game.battleengine.BattleEngineStrategy
import com.github.retro_game.retro_game.battleengine.Combatant
import com.github.retro_game.retro_game.battleengine.JavaBattleEngineStrategy
import com.github.retro_game.retro_game.battleengine.NativeBattleEngineStrategy
import io.javalin.Javalin
import io.javalin.json.JavalinJackson
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

private const val DEFAULT_PORT = 8078

fun main() {
  val mapper = JavalinJackson.defaultMapper()
    .registerModule(KotlinModule.Builder().build())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  val strategy = JavaBattleEngineStrategy()

  Javalin.create { config ->
    config.jsonMapper(JavalinJackson(mapper))
  }
    .post("/fight") { ctx ->
      val request = mapper.readValue<FightRequest>(ctx.body())
      val outcome = strategy.fight(request.attackers, request.defenders, request.rules, request.seed)
      ctx.json(outcome)
    }
    .get("/sim", BattleSimHttpApi(strategy))
    .get("/health") { ctx -> ctx.result("OK") }
    .start(readPort())
}

private fun readPort(): Int = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT

data class FightRequest(
  val attackers: List<Combatant>,
  val defenders: List<Combatant>,
  val rules: BattleRules,
  val seed: Int,
)
