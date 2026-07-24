package com.github.retro_game.retro_game.battleengineservice

import com.github.retro_game.retro_game.battleengine.UnitKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BattleSimQueryParserTest {
  @Test
  fun `exposes websim unit indexes in order`() {
    assertEquals(UnitKind.SMALL_CARGO, BattleSimUnits.unitsByIndex[0])
    assertEquals(UnitKind.BATTLE_CRUISER, BattleSimUnits.unitsByIndex[13])
    assertEquals(UnitKind.ROCKET_LAUNCHER, BattleSimUnits.unitsByIndex[14])
    assertEquals(UnitKind.LARGE_SHIELD_DOME, BattleSimUnits.unitsByIndex[21])
  }

  @Test
  fun `parses documented websim query into defender combatant`() {
    val input = BattleSimQueryParser.parse(
      mapOf(
        "enemy_pos" to listOf("2:100:8"),
        "enemy_metal" to listOf("750000"),
        "enemy_crystal" to listOf("300000"),
        "enemy_deut" to listOf("125000"),
        "ship_d0_5_b" to listOf("50"),
        "ship_d0_11_b" to listOf("12"),
        "ship_d0_13_b" to listOf("25"),
      ),
    )

    assertEquals(BattleSimResources(750000, 300000, 125000), input.resources)
    assertEquals(2, input.defender.coordinates().galaxy())
    assertEquals(100, input.defender.coordinates().system())
    assertEquals(8, input.defender.coordinates().position())
    assertEquals(0, input.defender.coordinates().kind())
    assertEquals(50, input.defender.unitGroups()[UnitKind.BATTLESHIP])
    assertEquals(12, input.defender.unitGroups()[UnitKind.DESTROYER])
    assertEquals(25, input.defender.unitGroups()[UnitKind.BATTLE_CRUISER])
  }

  @Test
  fun `parses technologies and dto-index battle cruiser alias`() {
    val input = BattleSimQueryParser.parse(
      mapOf(
        "enemy_pos" to listOf("4:321:10"),
        "ship_d0_13_b" to listOf("3"),
        "tech_d0_0" to listOf("13"),
        "tech_d0_1" to listOf("12"),
        "tech_d0_2" to listOf("14"),
      ),
    )

    assertEquals(13, input.defender.weaponsTechnology())
    assertEquals(12, input.defender.shieldingTechnology())
    assertEquals(14, input.defender.armorTechnology())
    assertEquals(3, input.defender.unitGroups()[UnitKind.BATTLE_CRUISER])
  }

  @Test
  fun `parses attacker units for fight input`() {
    val input = BattleSimQueryParser.parse(
      mapOf(
        "enemy_pos" to listOf("4:321:10"),
        "ship_a0_5_b" to listOf("10"),
        "tech_a0_0" to listOf("8"),
        "ship_d0_14_b" to listOf("25"),
        "seed" to listOf("123"),
      ),
    )

    assertEquals(123, input.seed)
    assertEquals(1, input.attackers.size)
    assertEquals(1, input.defenders.size)
    assertEquals(8, input.attackers.single().weaponsTechnology())
    assertEquals(10, input.attackers.single().unitGroups()[UnitKind.BATTLESHIP])
    assertEquals(25, input.defenders.single().unitGroups()[UnitKind.ROCKET_LAUNCHER])
  }

  @Test
  fun `rejects defensive units in attacker fleet`() {
    val error = assertFailsWith<AttackingFleetCannotContainDefensiveUnitsException> {
      BattleSimQueryParser.parse(
        mapOf(
          "enemy_pos" to listOf("4:321:10"),
          "ship_a0_14_b" to listOf("1"),
        ),
      )
    }

    assertEquals("Attacking fleet cannot contain defensive units.", error.message)
  }

  @Test
  fun `parses defense units and ignores zero counts`() {
    val input = BattleSimQueryParser.parse(
      mapOf(
        "enemy_pos" to listOf("3:42:9"),
        "ship_d0_14_b" to listOf("500"),
        "ship_d0_15_b" to listOf("250"),
        "ship_d0_19_b" to listOf("20"),
        "ship_d0_21_b" to listOf("0"),
      ),
    )

    assertEquals(500, input.defender.unitGroups()[UnitKind.ROCKET_LAUNCHER])
    assertEquals(250, input.defender.unitGroups()[UnitKind.LIGHT_LASER])
    assertEquals(20, input.defender.unitGroups()[UnitKind.PLASMA_TURRET])
    assertEquals(null, input.defender.unitGroups()[UnitKind.LARGE_SHIELD_DOME])
  }

  @Test
  fun `rejects malformed position`() {
    val error = assertFailsWith<IllegalArgumentException> {
      BattleSimQueryParser.parse(mapOf("enemy_pos" to listOf("1:2")))
    }

    assertEquals("enemy_pos must have format galaxy:system:position", error.message)
  }

  @Test
  fun `rejects negative numbers`() {
    val error = assertFailsWith<IllegalArgumentException> {
      BattleSimQueryParser.parse(
        mapOf(
          "enemy_pos" to listOf("1:2:3"),
          "ship_d0_5_b" to listOf("-1"),
        ),
      )
    }

    assertEquals("ship_d0_5_b must be non-negative", error.message)
  }
}
