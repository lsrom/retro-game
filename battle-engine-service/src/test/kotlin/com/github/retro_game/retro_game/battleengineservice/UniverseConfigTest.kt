package com.github.retro_game.retro_game.battleengineservice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UniverseConfigTest {
  @Test
  fun `uses defaults when environment is empty`() {
    val config = UniverseConfig.fromEnvironment(emptyMap())

    assertEquals(UniverseConfig(), config)
  }

  @Test
  fun `parses battle engine service environment variables`() {
    val config = UniverseConfig.fromEnvironment(
      mapOf(
        "BATTLE_ENGINE_SERVICE_FLEET_TO_DEBRIS" to "0.7",
        "BATTLE_ENGINE_SERVICE_DEFENSE_TO_DEBRIS" to "0.3",
        "BATTLE_ENGINE_SERVICE_MOONSHOT_DEBRIS_PER_UNIT" to "50000",
        "BATTLE_ENGINE_SERVICE_MOONSHOT_MAX_PERCENT" to "35",
        "BATTLE_ENGINE_SERVICE_USE_NATIVE_COMBAT_ENGINE" to "true",
      ),
    )

    assertEquals(0.7, config.fleetToDebris)
    assertEquals(0.3, config.defenseToDebris)
    assertEquals(50_000L, config.moonshotConfig.debrisPerUnit)
    assertEquals(35, config.moonshotConfig.maxPercent)
    assertTrue(config.useNativeCombatEngine)
  }

  @Test
  fun `parses retro game compatible environment variables`() {
    val config = UniverseConfig.fromEnvironment(
      mapOf(
        "RETRO_GAME_FLEET_DEBRIS_FACTOR" to "0.6",
        "RETRO_GAME_DEFENSE_DEBRIS_FACTOR" to "0.2",
        "RETRO_GAME_MOON_CHANCE_RESOURCE_PERCENT" to "75000",
        "RETRO_GAME_MAX_MOON_CHANCE" to "0.4",
        "RETRO_GAME_BATTLE_ENGINE" to "native",
      ),
    )

    assertEquals(0.6, config.fleetToDebris)
    assertEquals(0.2, config.defenseToDebris)
    assertEquals(75_000L, config.moonshotConfig.debrisPerUnit)
    assertEquals(40, config.moonshotConfig.maxPercent)
    assertTrue(config.useNativeCombatEngine)
  }

  @Test
  fun `service environment variables override retro game compatible variables`() {
    val config = UniverseConfig.fromEnvironment(
      mapOf(
        "RETRO_GAME_FLEET_DEBRIS_FACTOR" to "0.6",
        "BATTLE_ENGINE_SERVICE_FLEET_TO_DEBRIS" to "0.9",
        "RETRO_GAME_BATTLE_ENGINE" to "native",
        "BATTLE_ENGINE_SERVICE_USE_NATIVE_COMBAT_ENGINE" to "false",
      ),
    )

    assertEquals(0.9, config.fleetToDebris)
    assertFalse(config.useNativeCombatEngine)
  }

  @Test
  fun `rejects invalid environment variables`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      UniverseConfig.fromEnvironment(mapOf("BATTLE_ENGINE_SERVICE_DEFENSE_TO_DEBRIS" to "many"))
    }

    assertEquals(
      "BATTLE_ENGINE_SERVICE_DEFENSE_TO_DEBRIS or RETRO_GAME_DEFENSE_DEBRIS_FACTOR must be a decimal number, but was 'many'.",
      exception.message,
    )
  }
}
