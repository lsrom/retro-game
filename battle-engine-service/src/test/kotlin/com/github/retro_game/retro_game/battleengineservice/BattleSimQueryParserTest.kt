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
    assertEquals(false, BattleSimUnits.metadata.single { it.kind == "BATTLE_CRUISER" }.defensive)
    assertEquals(true, BattleSimUnits.metadata.single { it.kind == "ROCKET_LAUNCHER" }.defensive)
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
        "attacker_pos" to listOf("2:222:7"),
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
    assertEquals(2, input.attackers.single().coordinates().galaxy())
    assertEquals(222, input.attackers.single().coordinates().system())
    assertEquals(7, input.attackers.single().coordinates().position())
    assertEquals(10, input.attackers.single().unitGroups()[UnitKind.BATTLESHIP])
    assertEquals(25, input.defenders.single().unitGroups()[UnitKind.ROCKET_LAUNCHER])
  }

  @Test
  fun `parses all supported websim parameters together`() {
    val parameters = mutableMapOf(
      "attacker_pos" to listOf("1:23:7"),
      "enemy_pos" to listOf("2:100:8"),
      "enemy_metal" to listOf("100000"),
      "enemy_crystal" to listOf("50000"),
      "enemy_deut" to listOf("25000"),
      "tech_a0_0" to listOf("1"),
      "tech_a0_1" to listOf("2"),
      "tech_a0_2" to listOf("3"),
      "tech_d0_0" to listOf("4"),
      "tech_d0_1" to listOf("5"),
      "tech_d0_2" to listOf("6"),
      "seed" to listOf("987654321"),
    )
    val expectedAttackerUnits = mutableMapOf<UnitKind, Long>()
    val expectedDefenderUnits = mutableMapOf<UnitKind, Long>()

    for ((index, kind) in BattleSimUnits.unitsByIndex) {
      val attackerCount = if (kind in BattleSimUnits.defensiveUnitKinds) 0L else index + 1L
      val defenderCount = index + 101L
      parameters["ship_a0_${index}_b"] = listOf(attackerCount.toString())
      parameters["ship_d0_${index}_b"] = listOf(defenderCount.toString())
      if (attackerCount > 0) {
        expectedAttackerUnits[kind] = attackerCount
      }
      expectedDefenderUnits[kind] = defenderCount
    }

    val input = BattleSimQueryParser.parse(parameters)

    assertEquals(987654321, input.seed)
    assertEquals(BattleSimResources(100000, 50000, 25000), input.resources)
    assertEquals(1, input.attackers.size)
    assertEquals(1, input.defenders.size)

    assertEquals(1, input.attacker.coordinates().galaxy())
    assertEquals(23, input.attacker.coordinates().system())
    assertEquals(7, input.attacker.coordinates().position())
    assertEquals(0, input.attacker.coordinates().kind())
    assertEquals(1, input.attacker.weaponsTechnology())
    assertEquals(2, input.attacker.shieldingTechnology())
    assertEquals(3, input.attacker.armorTechnology())
    assertEquals(expectedAttackerUnits, input.attacker.unitGroups())

    assertEquals(2, input.defender.coordinates().galaxy())
    assertEquals(100, input.defender.coordinates().system())
    assertEquals(8, input.defender.coordinates().position())
    assertEquals(0, input.defender.coordinates().kind())
    assertEquals(4, input.defender.weaponsTechnology())
    assertEquals(5, input.defender.shieldingTechnology())
    assertEquals(6, input.defender.armorTechnology())
    assertEquals(expectedDefenderUnits, input.defender.unitGroups())
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
