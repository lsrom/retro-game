package com.github.retro_game.retro_game.battleengineservice

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.retro_game.retro_game.battleengine.BattleRules
import com.github.retro_game.retro_game.battleengine.Combatant
import com.github.retro_game.retro_game.battleengine.JavaBattleEngineStrategy
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse
import io.javalin.http.NotFoundResponse
import io.javalin.json.JavalinJackson
import java.util.UUID

private const val DEFAULT_PORT = 8078

fun main() {
  val mapper = JavalinJackson.defaultMapper()
    .registerModule(KotlinModule.Builder().build())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  val strategy = JavaBattleEngineStrategy()
  val historyDatabase = HistoryDatabase()

  Javalin.create { config ->
    config.jsonMapper(JavalinJackson(mapper))
  }
    .post("/fight") { ctx ->
      val request = mapper.readValue<FightRequest>(ctx.body())
      val outcome = strategy.fight(request.attackers, request.defenders, request.rules, request.seed)
      ctx.json(outcome)
    }
    .get("/sim-ui") { ctx ->
      ctx.contentType("text/html").result(classpathResourceText("public/sim-ui.html"))
    }
    .get("/sim-history-ui") { ctx ->
      ctx.contentType("text/html").result(classpathResourceText("public/sim-history-ui.html"))
    }
    .get("/sim-ui.js") { ctx ->
      ctx.contentType("application/javascript").result(classpathResourceText("public/sim-ui.js"))
    }
    .get("/sim-history-ui.js") { ctx ->
      ctx.contentType("application/javascript").result(classpathResourceText("public/sim-history-ui.js"))
    }
    .get("/sim-units") { ctx ->
      ctx.json(BattleSimUnits.metadata)
    }
    .get("/sim", BattleSimHttpApi(strategy, historyDatabase = historyDatabase))
    .get("/sim-history") { ctx ->
      val limit = ctx.queryParam("limit")?.toIntOrNull() ?: 100
      val offset = ctx.queryParam("offset")?.toIntOrNull() ?: 0
      ctx.json(historyDatabase.list(limit, offset))
    }
    .get("/sim-history/{id}") { ctx ->
      val id = try {
        UUID.fromString(ctx.pathParam("id"))
      } catch (_: IllegalArgumentException) {
        throw BadRequestResponse("Invalid history item id.")
      }
      ctx.json(historyDatabase.load(id) ?: throw NotFoundResponse("History item not found."))
    }
    .get("/health") { ctx -> ctx.result("OK") }
    .start(readPort())
}

private fun readPort(): Int = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT

private fun classpathResourceText(path: String): String =
  Thread.currentThread().contextClassLoader.getResource(path)?.readText()
    ?: throw IllegalStateException("Classpath resource not found: $path")

data class FightRequest(
  val attackers: List<Combatant>,
  val defenders: List<Combatant>,
  val rules: BattleRules,
  val seed: Int,
)
