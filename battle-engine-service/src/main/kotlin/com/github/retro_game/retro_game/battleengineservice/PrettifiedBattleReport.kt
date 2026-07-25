package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.UnitGroupStats
import com.github.retro_game.retro_game.battleengine.UnitKind
import io.javalin.http.BadRequestResponse
import java.text.NumberFormat
import java.util.EnumMap
import java.util.Locale

private const val DEFAULT_TEMPLATE_URL = "/sim-prettified-export-template.html"

internal object PrettifiedBattleReportTemplates {
  val templates: List<PrettifiedBattleReportTemplate> = listOf(
    PrettifiedBattleReportTemplate("Table", DEFAULT_TEMPLATE_URL, "public/sim-prettified-export-template.html"),
    PrettifiedBattleReportTemplate("Classic", "/sim-prettified-export-classic-template.html", "public/sim-prettified-export-classic-template.html"),
    PrettifiedBattleReportTemplate("Roster", "/sim-prettified-export-roster-template.html", "public/sim-prettified-export-roster-template.html"),
  )

  fun resourcePathFor(url: String?): String {
    val templateUrl = url?.takeIf { it.isNotBlank() } ?: DEFAULT_TEMPLATE_URL
    return templates.singleOrNull { it.url == templateUrl }?.resourcePath
      ?: throw BadRequestResponse("Unknown prettified report template: $templateUrl")
  }
}

data class PrettifiedBattleReportTemplate(
  val label: String,
  val url: String,
  val resourcePath: String,
)

data class PrettifiedBattleReportRequest(
  val output: SimOutput,
  val input: PrettifiedBattleReportInput,
  val templateUrl: String? = null,
)

data class PrettifiedBattleReportInput(
  val attacker: PrettifiedReportCombatant,
  val defender: PrettifiedReportCombatant,
)

data class PrettifiedReportCombatant(
  val title: String,
  val coordinates: String,
  val weaponsTechnology: Int,
  val shieldingTechnology: Int,
  val armorTechnology: Int,
  val unitGroups: Map<UnitKind, Long> = emptyMap(),
)

internal class PrettifiedBattleReportRenderer(
  private val templateLoader: (String) -> String,
  private val units: List<BattleSimUnitMetadata> = BattleSimUnits.metadata,
) {
  private val numberFormat = NumberFormat.getIntegerInstance(Locale.US)

  fun render(request: PrettifiedBattleReportRequest): String {
    val attackerFinalStats = request.output.lastRoundStats(Side.ATTACKER)
    val defenderFinalStats = request.output.lastRoundStats(Side.DEFENDER)
    val template = templateLoader(PrettifiedBattleReportTemplates.resourcePathFor(request.templateUrl))

    return SimpleHtmlTemplate.render(template, reportModel(request, attackerFinalStats, defenderFinalStats))
  }

  private fun reportModel(
    request: PrettifiedBattleReportRequest,
    attackerFinalStats: Map<UnitKind, UnitGroupStats>?,
    defenderFinalStats: Map<UnitKind, UnitGroupStats>?,
  ): Map<String, Any?> {
    val output = request.output
    return mapOf(
      "unitColorCsv" to unitColorCsv(),
      "attackerFleet" to tableCombatantModel(request.input.attacker, attackerFinalStats),
      "attackerInitialFleet" to classicCombatantModel(request.input.attacker, null),
      "attackerFinalFleet" to classicCombatantModel(request.input.attacker, attackerFinalStats),
      "attackerRosterInitial" to rosterCombatantModel(request.input.attacker, attackerFinalStats, final = false),
      "attackerRosterFinal" to rosterCombatantModel(request.input.attacker, attackerFinalStats, final = true),
      "attackerLosses" to prettifiedNumber(output.lossesAttacker.total()),
      "defenderFleet" to tableCombatantModel(request.input.defender, defenderFinalStats),
      "defenderInitialFleet" to classicCombatantModel(request.input.defender, null),
      "defenderFinalFleet" to classicCombatantModel(request.input.defender, defenderFinalStats),
      "defenderRosterInitial" to rosterCombatantModel(request.input.defender, defenderFinalStats, final = false),
      "defenderRosterFinal" to rosterCombatantModel(request.input.defender, defenderFinalStats, final = true),
      "defenderLosses" to prettifiedNumber(output.lossesDefender.total()),
      "lossesDefenderMetal" to prettifiedNumber(output.lossesDefender.metal),
      "lossesDefenderCrystal" to prettifiedNumber(output.lossesDefender.crystal),
      "lossesDefenderDeuterium" to prettifiedNumber(output.lossesDefender.deuterium),
      "debrisMetal" to prettifiedNumber(output.debris.metal),
      "debrisCrystal" to prettifiedNumber(output.debris.crystal),
      "moonchance" to formatPercent(output.moonchance),
      "attackerWins" to (output.result == BattleResult.AttackerWins),
      "defenderWins" to (output.result == BattleResult.DefenderWins),
      "draw" to (output.result == BattleResult.Draw),
      "possiblePlunderMetal" to prettifiedNumber(output.possiblePlunder.metal),
      "possiblePlunderCrystal" to prettifiedNumber(output.possiblePlunder.crystal),
      "possiblePlunderDeuterium" to prettifiedNumber(output.possiblePlunder.deuterium),
      "attackerProfitMetal" to prettifiedNumber(output.possiblePlunder.metal - output.lossesAttacker.metal),
      "attackerProfitCrystal" to prettifiedNumber(output.possiblePlunder.crystal - output.lossesAttacker.crystal),
      "attackerProfitDeuterium" to prettifiedNumber(output.possiblePlunder.deuterium - output.lossesAttacker.deuterium),
      "attackerProfitTotal" to prettifiedNumber(output.possiblePlunder.total() - output.lossesAttacker.total()),
    )
  }

  private fun tableCombatantModel(
    combatant: PrettifiedReportCombatant,
    finalStats: Map<UnitKind, UnitGroupStats>?,
  ): Map<String, Any?> {
    val reportUnits = reportUnitsForCombatant(combatant, finalStats)
    return combatantModel(combatant) + mapOf(
      "hasUnits" to reportUnits.isNotEmpty(),
      "hasSurvivors" to reportUnits.any { (finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: 0L) > 0L },
      "colspan" to reportUnits.size,
      "units" to reportUnits.map {
        val initialCount = combatant.unitCount(it)
        val finalCount = finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: 0L
        unitModel(it) + mapOf(
          "initialCount" to prettifiedNumber(initialCount),
          "lossCount" to prettifiedNumber(initialCount - finalCount),
          "finalCount" to prettifiedNumber(finalCount),
        )
      },
    )
  }

  private fun classicCombatantModel(
    combatant: PrettifiedReportCombatant,
    finalStats: Map<UnitKind, UnitGroupStats>?,
  ): Map<String, Any?> {
    val reportUnits = units.filter {
      if (finalStats == null) combatant.unitCount(it) > 0L else (finalStats[it.kindEnum()]?.numRemainingUnits() ?: 0L) > 0L
    }
    return combatantModel(combatant) + mapOf(
      "weaponsPercent" to combatant.weaponsTechnology * 10,
      "shieldingPercent" to combatant.shieldingTechnology * 10,
      "armorPercent" to combatant.armorTechnology * 10,
      "hasUnits" to reportUnits.isNotEmpty(),
      "units" to reportUnits.map {
        unitModel(it) + mapOf(
          "count" to prettifiedNumber(finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: combatant.unitCount(it)),
        )
      },
    )
  }

  private fun rosterCombatantModel(
    combatant: PrettifiedReportCombatant,
    finalStats: Map<UnitKind, UnitGroupStats>?,
    final: Boolean,
  ): Map<String, Any?> {
    val reportUnits = if (final) {
      reportUnitsForCombatant(combatant, finalStats)
    } else {
      units.filter { combatant.unitCount(it) > 0L }
    }
    return combatantModel(combatant) + mapOf(
      "hasUnits" to reportUnits.isNotEmpty(),
      "units" to reportUnits.map {
        val initialCount = combatant.unitCount(it)
        val finalCount = finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: 0L
        unitModel(it) + mapOf(
          "count" to prettifiedNumber(if (final) finalCount else initialCount),
          "final" to final,
          "lossCount" to prettifiedNumber(initialCount - finalCount),
        )
      },
    )
  }

  private fun combatantModel(combatant: PrettifiedReportCombatant): Map<String, Any?> =
    mapOf(
      "title" to combatant.title,
      "coordinates" to combatant.coordinates,
    )

  private fun unitModel(unit: BattleSimUnitMetadata): Map<String, Any?> {
    val preset = unitColorPreset(unit)
    return mapOf(
      "name" to unit.name,
      "abbreviation" to preset.abbreviation,
      "color" to preset.color,
    )
  }

  private fun reportUnitsForCombatant(
    combatant: PrettifiedReportCombatant,
    finalStats: Map<UnitKind, UnitGroupStats>?,
  ): List<BattleSimUnitMetadata> =
    units.filter { combatant.unitCount(it) > 0L || (finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: 0L) > 0L }

  private fun unitColorCsv(): String =
    buildString {
      appendLine("unit,abbreviation,color")
      for ((kind, abbreviation, color) in UNIT_COLOR_PRESETS) {
        appendLine("$kind,$abbreviation,$color")
      }
    }.trimEnd()

  private fun unitColorPreset(unit: BattleSimUnitMetadata): UnitColorPreset =
    UNIT_COLOR_PRESETS.firstOrNull { it.kind == unit.kind } ?: UnitColorPreset(unit.kind, unit.name, "#aeb7c5")

  private fun prettifiedNumber(value: Long): String = numberFormat.format(value).replace(',', '.')

  private fun formatPercent(value: Double): String = "%.2f%%".format(Locale.US, value)

  private fun BattleSimUnitMetadata.kindEnum(): UnitKind = UnitKind.valueOf(kind)

  private fun PrettifiedReportCombatant.unitCount(unit: BattleSimUnitMetadata): Long = unitGroups[unit.kindEnum()] ?: 0L

  private fun SimOutput.lastRoundStats(side: Side): EnumMap<UnitKind, UnitGroupStats>? {
    val outcomes = when (side) {
      Side.ATTACKER -> outcome.attackersOutcomes()
      Side.DEFENDER -> outcome.defendersOutcomes()
    }
    return outcomes.firstOrNull()?.unitGroupsStats()?.lastOrNull()
  }

  private fun Resources.total(): Long = metal + crystal + deuterium

  private enum class Side {
    ATTACKER,
    DEFENDER,
  }
}

private object SimpleHtmlTemplate {
  private val sectionRegex = Regex("""\{\{([#^])([A-Za-z0-9_.]+)}}(.*?)\{\{/\2}}""", RegexOption.DOT_MATCHES_ALL)
  private val variableRegex = Regex("""\{\{([A-Za-z0-9_.]+)}}""")

  fun render(template: String, model: Map<String, Any?>): String = render(template, listOf(model))

  private fun render(template: String, contexts: List<Any?>): String {
    val withSections = replaceSections(template, contexts)
    return variableRegex.replace(withSections) { match ->
      escapeHtml(resolve(match.groupValues[1], contexts)?.toString() ?: "")
    }
  }

  private fun replaceSections(template: String, contexts: List<Any?>): String {
    var html = template
    while (true) {
      val match = sectionRegex.find(html) ?: return html
      val marker = match.groupValues[1]
      val key = match.groupValues[2]
      val block = match.groupValues[3]
      val value = resolve(key, contexts)
      val replacement = if (marker == "#") renderTruthySection(value, block, contexts) else renderInvertedSection(value, block, contexts)
      html = html.replaceRange(match.range, replacement)
    }
  }

  private fun renderTruthySection(value: Any?, block: String, contexts: List<Any?>): String =
    when (value) {
      is Iterable<*> -> value.joinToString("") { render(block, listOf(it) + contexts) }
      is Boolean -> if (value) render(block, contexts) else ""
      is Map<*, *> -> if (value.isEmpty()) "" else render(block, listOf(value) + contexts)
      null -> ""
      else -> render(block, contexts)
    }

  private fun renderInvertedSection(value: Any?, block: String, contexts: List<Any?>): String =
    if (isTruthy(value)) "" else render(block, contexts)

  private fun isTruthy(value: Any?): Boolean =
    when (value) {
      null -> false
      is Boolean -> value
      is Iterable<*> -> value.any()
      is Map<*, *> -> value.isNotEmpty()
      else -> true
    }

  private fun resolve(path: String, contexts: List<Any?>): Any? {
    for (context in contexts) {
      val value = resolveInContext(path, context)
      if (value != MissingTemplateValue) {
        return value
      }
    }
    return null
  }

  private fun resolveInContext(path: String, context: Any?): Any? {
    var value: Any? = context
    for (part in path.split('.')) {
      value = when (value) {
        is Map<*, *> -> if (value.containsKey(part)) value[part] else return MissingTemplateValue
        else -> return MissingTemplateValue
      }
    }
    return value
  }

  private fun escapeHtml(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")

  private object MissingTemplateValue
}

private data class UnitColorPreset(
  val kind: String,
  val abbreviation: String,
  val color: String,
)

private val UNIT_COLOR_PRESETS = listOf(
  UnitColorPreset("SMALL_CARGO", "S.Cargo", "#d7aa63"),
  UnitColorPreset("LARGE_CARGO", "L.Cargo", "#d7aa63"),
  UnitColorPreset("LITTLE_FIGHTER", "L.Fighter", "#93d66b"),
  UnitColorPreset("HEAVY_FIGHTER", "H.Fighter", "#93d66b"),
  UnitColorPreset("CRUISER", "Cruiser", "#59b4d8"),
  UnitColorPreset("BATTLESHIP", "B.Ship", "#e9a93b"),
  UnitColorPreset("COLONY_SHIP", "Col. Ship", "#d7aa63"),
  UnitColorPreset("RECYCLER", "Recycler", "#8ed0d8"),
  UnitColorPreset("ESPIONAGE_PROBE", "Probe", "#b9c7d8"),
  UnitColorPreset("BOMBER", "Bomber", "#d384e6"),
  UnitColorPreset("SOLAR_SATELLITE", "Sol. Sat", "#00a99d"),
  UnitColorPreset("DESTROYER", "Dest.", "#f06c64"),
  UnitColorPreset("DEATH_STAR", "Rip", "#d9dce7"),
  UnitColorPreset("BATTLE_CRUISER", "B.Cruiser", "#7cc8ff"),
  UnitColorPreset("ROCKET_LAUNCHER", "Miss.", "#95d979"),
  UnitColorPreset("LIGHT_LASER", "S.Laser", "#d7aa63"),
  UnitColorPreset("HEAVY_LASER", "H.Laser", "#93d66b"),
  UnitColorPreset("GAUSS_CANNON", "Gauss", "#b020b8"),
  UnitColorPreset("ION_CANNON", "Ion.C", "#a8b5c8"),
  UnitColorPreset("PLASMA_TURRET", "Plasma", "#baf0a4"),
  UnitColorPreset("SMALL_SHIELD_DOME", "S.Dome", "#d7aa63"),
  UnitColorPreset("LARGE_SHIELD_DOME", "LS.Dome", "#d7aa63"),
)
