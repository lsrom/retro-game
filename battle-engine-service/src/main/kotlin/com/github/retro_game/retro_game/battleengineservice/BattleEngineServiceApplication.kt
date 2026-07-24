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

private const val DEFAULT_PORT = 8080
private const val BATTLE_ENGINE_PROPERTY = "retro-game.battle-engine"

fun main() {
  val mapper = JavalinJackson.defaultMapper()
    .registerModule(KotlinModule.Builder().build())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  val strategy = battleEngineStrategy(loadBattleEngineType())

  Javalin.create { config ->
    config.jsonMapper(JavalinJackson(mapper))
  }
    .post("/fight") { ctx ->
      val request = mapper.readValue<FightRequest>(ctx.body())
      val outcome = strategy.fight(request.attackers, request.defenders, request.rules, request.seed)
      ctx.json(outcome)
    }
    .get("/health") { ctx -> ctx.result("OK") }
    .start(readPort())
}

private fun loadBattleEngineType(): String {
  val configPath = Path.of("config", "application.properties")
  require(Files.isRegularFile(configPath)) {
    "Missing $configPath; $BATTLE_ENGINE_PROPERTY must be configured in the main application.properties"
  }

  val properties = Properties()
  Files.newInputStream(configPath).use(properties::load)
  val value = properties.getProperty(BATTLE_ENGINE_PROPERTY)
  require(!value.isNullOrBlank()) {
    "Missing $BATTLE_ENGINE_PROPERTY in $configPath"
  }
  return value
}

private fun battleEngineStrategy(type: String): BattleEngineStrategy =
  when (type) {
    "java" -> JavaBattleEngineStrategy()
    "native" -> NativeBattleEngineStrategy()
    else -> throw IllegalArgumentException("$BATTLE_ENGINE_PROPERTY must be java or native, got '$type'")
  }

private fun readPort(): Int = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT

data class FightRequest(
  val attackers: List<Combatant>,
  val defenders: List<Combatant>,
  val rules: BattleRules,
  val seed: Int,
)
