package com.github.retro_game.retro_game.battleengineservice

data class UniverseConfig(
    val fleetToDebris: Double = 0.3,
    val defenseToDebris: Double = 0.0,
    val moonshotConfig: MoonshotConfig = MoonshotConfig(
        debrisPerUnit = 100_000,
        maxPercent = 20
    ),
    val useNativeCombatEngine: Boolean = false
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): UniverseConfig {
            val defaults = UniverseConfig()
            return UniverseConfig(
                fleetToDebris = environment.doubleValue(
                    "BATTLE_ENGINE_SERVICE_FLEET_TO_DEBRIS",
                    "RETRO_GAME_FLEET_DEBRIS_FACTOR",
                    default = defaults.fleetToDebris,
                ),
                defenseToDebris = environment.doubleValue(
                    "BATTLE_ENGINE_SERVICE_DEFENSE_TO_DEBRIS",
                    "RETRO_GAME_DEFENSE_DEBRIS_FACTOR",
                    default = defaults.defenseToDebris,
                ),
                moonshotConfig = MoonshotConfig(
                    debrisPerUnit = environment.longValue(
                        "BATTLE_ENGINE_SERVICE_MOONSHOT_DEBRIS_PER_UNIT",
                        "RETRO_GAME_MOON_CHANCE_RESOURCE_PERCENT",
                        default = defaults.moonshotConfig.debrisPerUnit,
                    ),
                    maxPercent = environment.moonshotMaxPercent(defaults.moonshotConfig.maxPercent),
                ),
                useNativeCombatEngine = environment.booleanValue(
                    "BATTLE_ENGINE_SERVICE_USE_NATIVE_COMBAT_ENGINE",
                    default = environment["RETRO_GAME_BATTLE_ENGINE"]?.equals("native", ignoreCase = true)
                        ?: defaults.useNativeCombatEngine,
                ),
            )
        }
    }
}

data class MoonshotConfig(
    val debrisPerUnit: Long,
    val maxPercent: Int
)

private fun Map<String, String>.doubleValue(primaryName: String, fallbackName: String, default: Double): Double =
    value(primaryName, fallbackName)?.toDoubleOrNull()
        ?: value(primaryName, fallbackName)?.let { invalidConfigValue(primaryName, fallbackName, it, "a decimal number") }
        ?: default

private fun Map<String, String>.longValue(primaryName: String, fallbackName: String, default: Long): Long =
    value(primaryName, fallbackName)?.toLongOrNull()
        ?: value(primaryName, fallbackName)?.let { invalidConfigValue(primaryName, fallbackName, it, "an integer") }
        ?: default

private fun Map<String, String>.booleanValue(name: String, default: Boolean): Boolean =
    when (this[name]?.trim()?.lowercase()) {
        null -> default
        "true", "1", "yes", "y", "on" -> true
        "false", "0", "no", "n", "off" -> false
        else -> invalidConfigValue(name, null, this[name].orEmpty(), "a boolean")
    }

private fun Map<String, String>.moonshotMaxPercent(default: Int): Int {
    val maxPercent = this["BATTLE_ENGINE_SERVICE_MOONSHOT_MAX_PERCENT"]
    if (maxPercent != null) {
        return maxPercent.toIntOrNull()
            ?: invalidConfigValue("BATTLE_ENGINE_SERVICE_MOONSHOT_MAX_PERCENT", null, maxPercent, "an integer percent")
    }

    val maxChance = this["RETRO_GAME_MAX_MOON_CHANCE"] ?: return default
    return maxChance.toDoubleOrNull()
        ?.let { (it * 100).toInt() }
        ?: invalidConfigValue("RETRO_GAME_MAX_MOON_CHANCE", null, maxChance, "a decimal chance from 0.0 to 1.0")
}

private fun Map<String, String>.value(primaryName: String, fallbackName: String): String? =
    this[primaryName] ?: this[fallbackName]

private fun invalidConfigValue(primaryName: String, fallbackName: String?, value: String, expected: String): Nothing {
    val names = listOfNotNull(primaryName, fallbackName).joinToString(" or ")
    throw IllegalArgumentException("$names must be $expected, but was '$value'.")
}
