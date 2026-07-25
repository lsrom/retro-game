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

    return renderTemplate(
      template,
      mapOf(
        "unitColorCsv" to escapeHtml(unitColorCsv()),
        "attackerFleet" to renderPrettifiedCombatant(request.input.attacker, attackerFinalStats),
        "attackerInitialFleet" to renderClassicInitialCombatant(request.input.attacker),
        "attackerFinalFleet" to renderClassicFinalCombatant(request.input.attacker, attackerFinalStats),
        "attackerRosterInitial" to renderRosterCombatant(request.input.attacker, attackerFinalStats, Side.ATTACKER, final = false),
        "attackerRosterFinal" to renderRosterCombatant(request.input.attacker, attackerFinalStats, Side.ATTACKER, final = true),
        "attackerLosses" to prettifiedNumber(request.output.lossesAttacker.total()),
        "defenderFleet" to renderPrettifiedCombatant(request.input.defender, defenderFinalStats),
        "defenderInitialFleet" to renderClassicInitialCombatant(request.input.defender),
        "defenderFinalFleet" to renderClassicFinalCombatant(request.input.defender, defenderFinalStats),
        "defenderRosterInitial" to renderRosterCombatant(request.input.defender, defenderFinalStats, Side.DEFENDER, final = false),
        "defenderRosterFinal" to renderRosterCombatant(request.input.defender, defenderFinalStats, Side.DEFENDER, final = true),
        "outcome" to prettifiedOutcomeHtml(request.output),
        "rosterSummary" to renderRosterSummary(request.output),
        "defenderLosses" to prettifiedNumber(request.output.lossesDefender.total()),
        "debrisMetal" to prettifiedNumber(request.output.debris.metal),
        "debrisCrystal" to prettifiedNumber(request.output.debris.crystal),
        "moonchance" to formatPercent(request.output.moonchance),
      ),
    )
  }

  private fun renderTemplate(template: String, values: Map<String, String>): String =
    values.entries.fold(template) { html, (key, value) -> html.replace("{{$key}}", value) }

  private fun renderPrettifiedCombatant(
    combatant: PrettifiedReportCombatant,
    finalStats: Map<UnitKind, UnitGroupStats>?,
  ): String {
    val reportUnits = reportUnitsForCombatant(combatant, finalStats)
    if (reportUnits.isEmpty()) {
      return """
        <section class="combatant">
          <div><span class="role">${escapeHtml(combatant.title)}</span> (${escapeHtml(combatant.coordinates)})</div>
          <div class="empty">No units</div>
        </section>
      """.trimIndent()
    }

    val hasSurvivors = reportUnits.any { (finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: 0L) > 0L }
    val headerCells = reportUnits.joinToString("") { renderTableCell("th", it, unitColorPreset(it).abbreviation) }
    val initialCells = renderCountCells(reportUnits) { combatant.unitCount(it) }
    val lossCells = renderLossCells(reportUnits, combatant, finalStats)
    val finalCells = renderCountCells(reportUnits) { finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: 0L }
    val finalRows = if (hasSurvivors) {
      "<tr>$lossCells</tr><tr>$finalCells</tr>"
    } else {
      """<tr><td class="destroyed" colspan="${reportUnits.size}">Destroyed</td></tr>"""
    }

    return """
      <section class="combatant">
        <div><span class="role">${escapeHtml(combatant.title)}</span> (${escapeHtml(combatant.coordinates)})</div>
        <table>
          <thead>
            <tr>$headerCells</tr>
          </thead>
          <tbody>
            <tr>$initialCells</tr>
            $finalRows
          </tbody>
        </table>
      </section>
    """.trimIndent()
  }

  private fun renderTableCell(tagName: String, unit: BattleSimUnitMetadata, value: String, className: String = ""): String {
    val preset = unitColorPreset(unit)
    return """<$tagName class="$className" style="color:${escapeHtml(preset.color)}">${escapeHtml(value)}</$tagName>"""
  }

  private fun renderCountCells(
    reportUnits: List<BattleSimUnitMetadata>,
    className: String = "",
    countForUnit: (BattleSimUnitMetadata) -> Long,
  ): String =
    reportUnits.joinToString("") { renderTableCell("td", it, prettifiedNumber(countForUnit(it)), className) }

  private fun renderLossCells(
    reportUnits: List<BattleSimUnitMetadata>,
    combatant: PrettifiedReportCombatant,
    finalStats: Map<UnitKind, UnitGroupStats>?,
  ): String =
    reportUnits.joinToString("") {
      val initialCount = combatant.unitCount(it)
      val finalCount = finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: 0L
      renderTableCell("td", it, "-${prettifiedNumber(initialCount - finalCount)}", "loss")
    }

  private fun renderClassicInitialCombatant(combatant: PrettifiedReportCombatant): String {
    val reportUnits = units.filter { combatant.unitCount(it) > 0L }
    return """
      <section class="combatant">
        <div><span class="role">${escapeHtml(combatant.title)}</span> (${escapeHtml(combatant.coordinates)})</div>
        <div>Weapons: ${combatant.weaponsTechnology * 10}% Shields: ${combatant.shieldingTechnology * 10}% Hull Plating: ${combatant.armorTechnology * 10}%</div>
        ${renderClassicUnitLines(reportUnits) { combatant.unitCount(it) }}
      </section>
    """.trimIndent()
  }

  private fun renderClassicFinalCombatant(
    combatant: PrettifiedReportCombatant,
    finalStats: Map<UnitKind, UnitGroupStats>?,
  ): String {
    val reportUnits = units.filter { (finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: 0L) > 0L }
    return """
      <section class="combatant">
        <div><span class="role">${escapeHtml(combatant.title)}</span> (${escapeHtml(combatant.coordinates)})</div>
        ${renderClassicUnitLines(reportUnits) { finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: 0L }}
      </section>
    """.trimIndent()
  }

  private fun renderClassicUnitLines(
    reportUnits: List<BattleSimUnitMetadata>,
    countForUnit: (BattleSimUnitMetadata) -> Long,
  ): String {
    if (reportUnits.isEmpty()) {
      return """
        <div class="unit-line">Type: -</div>
        <div class="unit-line">Number: -</div>
      """.trimIndent()
    }

    val labels = reportUnits.joinToString(" ") {
      val preset = unitColorPreset(it)
      coloredSpan(preset.abbreviation, preset.color)
    }
    val counts = reportUnits.joinToString(" ") {
      val preset = unitColorPreset(it)
      coloredSpan(prettifiedNumber(countForUnit(it)), preset.color)
    }

    return """
      <div class="unit-line">Type: $labels</div>
      <div class="unit-line">Number: $counts</div>
    """.trimIndent()
  }

  private fun renderRosterCombatant(
    combatant: PrettifiedReportCombatant,
    finalStats: Map<UnitKind, UnitGroupStats>?,
    side: Side,
    final: Boolean,
  ): String {
    val reportUnits = if (final) {
      reportUnitsForCombatant(combatant, finalStats)
    } else {
      units.filter { combatant.unitCount(it) > 0L }
    }
    val separatorClass = if (side == Side.ATTACKER) "attacker-separator" else "defender-separator"
    val unitLines = if (reportUnits.isEmpty()) {
      """<div class="empty">No units</div>"""
    } else {
      reportUnits.joinToString("\n") {
        val initialCount = combatant.unitCount(it)
        val finalCount = finalStats?.get(it.kindEnum())?.numRemainingUnits() ?: 0L
        val count = if (final) finalCount else initialCount
        val loss = if (final) " (-${prettifiedNumber(initialCount - finalCount)})" else ""
        """<div class="${side.lineClass()}">${escapeHtml(it.name)} ${prettifiedNumber(count)}$loss</div>"""
      }
    }

    return """
      <section class="section">
        <div class="combatant-title"><span class="${side.labelClass()}">${escapeHtml(combatant.title)}</span> ${escapeHtml(combatant.coordinates)}</div>
        $unitLines
        <div class="separator $separatorClass">------------------------------------------------------------</div>
      </section>
    """.trimIndent()
  }

  private fun renderRosterSummary(output: SimOutput): String =
    """
      <section class="summary">
        <div>${rosterOutcomeHtml(output)}</div>
        <div>The attacker lost a total of <span class="number">${prettifiedNumber(output.lossesAttacker.total())}</span> units.</div>
        <div>The defender lost a total of <span class="number">${prettifiedNumber(output.lossesDefender.total())}</span> units.</div>
        <br>
        <div>At these space coordinates now float <span class="number">${prettifiedNumber(output.debris.metal)}</span> metal and <span class="number">${prettifiedNumber(output.debris.crystal)}</span> crystal.</div>
        <div>The chance for a moon to be created from the debris was <span class="number">${formatPercent(output.moonchance)}</span>.</div>
        <div class="summary-title">Summary attackers(s)</div>
        <div>Metal: <span class="profit">${prettifiedNumber(output.possiblePlunder.metal - output.lossesAttacker.metal)}</span></div>
        <div>Crystal: <span class="profit">${prettifiedNumber(output.possiblePlunder.crystal - output.lossesAttacker.crystal)}</span></div>
        <div>Deuterium: <span class="profit">${prettifiedNumber(output.possiblePlunder.deuterium - output.lossesAttacker.deuterium)}</span></div>
        <div>The attacker(s) made a profit of <span class="profit">${prettifiedNumber(output.possiblePlunder.total() - output.lossesAttacker.total())}</span> units.</div>
        <div class="summary-title">Summary defenders(s)</div>
        <div>Metal: <span class="loss">-${prettifiedNumber(output.lossesDefender.metal)}</span></div>
        <div>Crystal: <span class="loss">-${prettifiedNumber(output.lossesDefender.crystal)}</span></div>
        <div>Deuterium: <span class="loss">-${prettifiedNumber(output.lossesDefender.deuterium)}</span></div>
        <div>The defender(s) lost a total of <span class="number">${prettifiedNumber(output.lossesDefender.total())}</span> units.</div>
      </section>
    """.trimIndent()

  private fun prettifiedOutcomeHtml(output: SimOutput): String =
    when (output.result) {
      BattleResult.AttackerWins ->
        """Attacker captures <span class="number">${prettifiedNumber(output.possiblePlunder.metal)}</span> Metal, <span class="number">${prettifiedNumber(output.possiblePlunder.crystal)}</span> Crystal and <span class="number">${prettifiedNumber(output.possiblePlunder.deuterium)}</span> Deuterium."""

      BattleResult.DefenderWins -> "The defender has won the battle."
      BattleResult.Draw -> "The battle ended in a draw."
    }

  private fun rosterOutcomeHtml(output: SimOutput): String =
    when (output.result) {
      BattleResult.AttackerWins ->
        """The attacker captured <span class="number">${prettifiedNumber(output.possiblePlunder.metal)}</span> Metal, <span class="number">${prettifiedNumber(output.possiblePlunder.crystal)}</span> Crystal and <span class="number">${prettifiedNumber(output.possiblePlunder.deuterium)}</span> Deuterium."""

      BattleResult.DefenderWins -> "The defender has won the battle."
      BattleResult.Draw -> "The battle ends in a draw."
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

  private fun coloredSpan(text: String, color: String): String =
    """<span style="color:${escapeHtml(color)}">${escapeHtml(text)}</span>"""

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

  private fun escapeHtml(value: Any?): String =
    value.toString()
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")

  private fun Side.labelClass(): String = if (this == Side.ATTACKER) "attacker-label" else "defender-label"

  private fun Side.lineClass(): String = if (this == Side.ATTACKER) "attacker-line" else "defender-line"

  private enum class Side {
    ATTACKER,
    DEFENDER,
  }
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
