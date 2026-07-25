package com.github.retro_game.retro_game.battleengineservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.retro_game.retro_game.battleengine.BattleOutcome
import com.github.retro_game.retro_game.battleengine.CombatantOutcome
import com.github.retro_game.retro_game.battleengine.UnitGroupStats
import com.github.retro_game.retro_game.battleengine.UnitKind
import io.javalin.http.BadRequestResponse
import java.util.EnumMap
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class PrettifiedBattleReportRendererTest {

  @Test
  fun `renders table report on the backend using selected template`() {
    val renderer = PrettifiedBattleReportRenderer(::classpathResourceText)

    val html = renderer.render(reportRequest())

    assertContains(html, """<span class="role">Attacker &lt;One&gt;</span> (1:2:3)""")
    assertContains(html, """<th class="" style="color:#d7aa63">S.Cargo</th>""")
    assertContains(html, """<td class="" style="color:#d7aa63">2.500</td>""")
    assertContains(html, """<td class="loss" style="color:#d7aa63">-1.000</td>""")
    assertContains(html, """Attacker captures <span class="number">1.500</span> Metal""")
    assertContains(html, """The attacker has lost a total of <span class="number">4.000</span> Units.""")
    assertContains(html, """The defender has lost a total of <span class="number">2.000</span> Units.""")
    assertContains(html, """At these space coordinates now float <span class="number">600</span> Metal and <span class="number">300</span> Crystal.""")
    assertContains(html, """The chance for a moon to arise from the debris is <span class="number">12.35%</span>.""")
    assertContains(html, "SMALL_CARGO,S.Cargo,#d7aa63")
    assertFalse(html.contains("{{"))
  }

  @Test
  fun `renders roster report placeholders`() {
    val renderer = PrettifiedBattleReportRenderer(::classpathResourceText)

    val html = renderer.render(reportRequest(templateUrl = "/sim-prettified-export-roster-template.html"))

    assertContains(html, """<span class="attacker-label">Attacker &lt;One&gt;</span> 1:2:3""")
    assertContains(html, """<div class="attacker-line">Small Cargo 1.500 (-1.000)</div>""")
    assertContains(html, """The attacker captured <span class="number">1.500</span> Metal""")
    assertContains(html, """The defender(s) lost a total of <span class="number">2.000</span> units.""")
    assertFalse(html.contains("{{"))
  }

  @Test
  fun `rejects unknown template urls`() {
    val renderer = PrettifiedBattleReportRenderer(templateLoader = { error("Template should not be loaded") })

    val error = assertFailsWith<BadRequestResponse> {
      renderer.render(reportRequest(templateUrl = "/../../secret.html"))
    }

    assertEquals("Unknown prettified report template: /../../secret.html", error.message)
  }

  @Test
  fun `report request supports json posted by browser export`() {
    val mapper = jacksonObjectMapper()
    val json = mapper.writeValueAsString(reportRequest())

    val request = mapper.readValue<PrettifiedBattleReportRequest>(json)

    assertEquals(7, request.output.outcome.seed())
    assertEquals(2_500L, request.input.attacker.unitGroups[UnitKind.SMALL_CARGO])
    assertEquals(1_500L, request.output.outcome.attackersOutcomes()[0].unitGroupsStats()[0][UnitKind.SMALL_CARGO]?.numRemainingUnits())
  }

  private fun reportRequest(templateUrl: String? = null): PrettifiedBattleReportRequest =
    PrettifiedBattleReportRequest(
      output = SimOutput(
        outcome = BattleOutcome(
          7,
          1,
          listOf(combatantOutcome(UnitKind.SMALL_CARGO to 1_500L)),
          listOf(combatantOutcome(UnitKind.ROCKET_LAUNCHER to 0L)),
        ),
        result = BattleResult.AttackerWins,
        debris = Resources(metal = 600L, crystal = 300L, deuterium = 0L),
        moonchance = 12.345,
        possiblePlunder = Resources(metal = 1_500L, crystal = 750L, deuterium = 250L),
        lossesAttacker = Resources(metal = 3_000L, crystal = 1_000L, deuterium = 0L),
        lossesDefender = Resources(metal = 2_000L, crystal = 0L, deuterium = 0L),
        elapsedTime = 42L,
      ),
      input = PrettifiedBattleReportInput(
        attacker = PrettifiedReportCombatant(
          title = "Attacker <One>",
          coordinates = "1:2:3",
          weaponsTechnology = 10,
          shieldingTechnology = 9,
          armorTechnology = 8,
          unitGroups = mapOf(UnitKind.SMALL_CARGO to 2_500L),
        ),
        defender = PrettifiedReportCombatant(
          title = "Defender",
          coordinates = "2:3:4",
          weaponsTechnology = 7,
          shieldingTechnology = 6,
          armorTechnology = 5,
          unitGroups = mapOf(UnitKind.ROCKET_LAUNCHER to 1L),
        ),
      ),
      templateUrl = templateUrl,
    )

  private fun combatantOutcome(vararg remainingUnits: Pair<UnitKind, Long>): CombatantOutcome =
    CombatantOutcome(
      listOf(
        EnumMap<UnitKind, UnitGroupStats>(UnitKind::class.java).also { stats ->
          for (kind in UnitKind.entries) {
            val count = remainingUnits.firstOrNull { it.first == kind }?.second ?: 0L
            stats[kind] = UnitGroupStats(count, 0L, 0L, 0f, 0f, 0f, 0f)
          }
        },
      ),
    )
}
