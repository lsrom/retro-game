package com.github.retro_game.retro_game.battleengine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.attributes;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.combatant;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.remaining;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.rules;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.stats;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

class JavaBattleEngineStrategyTest {
  private final JavaBattleEngineStrategy strategy = new JavaBattleEngineStrategy();

  @Test
  void returnsZeroRoundsWhenEitherSideIsEmptyWhenCalledDirectly() {
    var defender = combatant(UnitKind.SMALL_CARGO, 1);
    var attacker = combatant(UnitKind.BATTLESHIP, 1);
    var rules = rules(10, 0, 1_000);

    assertThat(strategy.fight(List.of(), List.of(defender), rules, 1).numRounds()).isZero();
    assertThat(strategy.fight(List.of(attacker), List.of(), rules, 1).numRounds()).isZero();
    assertThat(strategy.fight(List.of(), List.of(), rules, 1).numRounds()).isZero();
  }

  @Test
  void validatesBattleRulesAndUnitAttributesShape() {
    assertThatThrownBy(() -> new BattleRules(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("unitsAttributes cannot be null");
    assertThatThrownBy(() -> new BattleRules(new UnitAttributes[1]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("unitsAttributes must have one entry per unit kind");
    assertThatThrownBy(() -> new UnitAttributes(1, 1, 1, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("rapidFire cannot be null");
    assertThatThrownBy(() -> new UnitAttributes(1, 1, 1, new int[1]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("rapidFire must have one entry per unit kind");
  }

  @Test
  void normalizesZeroAndMinimumIntegerSeedsToOne() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 3));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 5));
    var rules = rules(10, 10, 1_000);

    var seedOne = strategy.fight(attackers, defenders, rules, 1);

    assertThat(strategy.fight(attackers, defenders, rules, 0)).isEqualTo(seedOne);
    assertThat(strategy.fight(attackers, defenders, rules, Integer.MIN_VALUE)).isEqualTo(seedOne);
  }

  @Test
  void normalizesNegativeSeedsToTheirPositiveValue() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 3));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 5));
    var rules = rules(10, 10, 1_000);

    assertThat(strategy.fight(attackers, defenders, rules, -12345))
        .isEqualTo(strategy.fight(attackers, defenders, rules, 12345));
  }

  @Test
  void isDeterministicAndDoesNotMutateInputs() {
    var attackerGroups = BattleEngineTestFixtures.groups(Map.of(UnitKind.BATTLESHIP, 2L, UnitKind.CRUISER, 1L));
    var defenderGroups = BattleEngineTestFixtures.groups(Map.of(UnitKind.SMALL_CARGO, 4L));
    var attacker = new Combatant(1, BattleEngineTestFixtures.COORDINATES, 0, 0, 0, attackerGroups);
    var defender = new Combatant(2, BattleEngineTestFixtures.COORDINATES, 0, 0, 0, defenderGroups);
    var attackers = List.of(attacker);
    var defenders = List.of(defender);
    var rules = rules(10, 10, 1_000);

    var first = strategy.fight(attackers, defenders, rules, 77);
    var second = strategy.fight(attackers, defenders, rules, 77);

    assertThat(second).isEqualTo(first);
    assertThat(attackerGroups).containsEntry(UnitKind.BATTLESHIP, 2L).containsEntry(UnitKind.CRUISER, 1L);
    assertThat(defenderGroups).containsEntry(UnitKind.SMALL_CARGO, 4L);
  }

  @Test
  void runsExactlySixRoundsWhenNeitherSideCanDamageTheOther() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 1));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var rules = rules(0, 0, 1_000);

    var outcome = strategy.fight(attackers, defenders, rules, 1);

    assertThat(outcome.numRounds()).isEqualTo(6);
    for (var round = 0; round < outcome.numRounds(); round++) {
      assertThat(remaining(outcome, true, round)).isEqualTo(1);
      assertThat(remaining(outcome, false, round)).isEqualTo(1);
      assertThat(stats(outcome, true, 0, round, UnitKind.BATTLESHIP).timesFired()).isEqualTo(1);
      assertThat(stats(outcome, false, 0, round, UnitKind.SMALL_CARGO).timesFired()).isEqualTo(1);
    }
  }

  @Test
  void defenderDestroyedByAttackerStillFiresBeforeRoundCleanup() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 1));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var rules = rules(Map.of(
        UnitKind.BATTLESHIP, attributes(100, 0, 1_000),
        UnitKind.SMALL_CARGO, attributes(0, 0, 100)
    ), 0, 0, 1_000);

    var outcome = strategy.fight(attackers, defenders, rules, 1);

    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(remaining(outcome, true, 0)).isEqualTo(1);
    assertThat(remaining(outcome, false, 0)).isZero();
    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).timesFired()).isEqualTo(1);
    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).hullDamageDealt()).isEqualTo(10.0f);
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).timesFired()).isEqualTo(1);
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).timesWasShot()).isEqualTo(1);
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isEqualTo(10.0f);
  }

  @Test
  void attackerCanFireBeforeBeingDestroyedByDefender() {
    var attackers = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var defenders = List.of(combatant(UnitKind.BATTLESHIP, 1));
    var rules = rules(Map.of(
        UnitKind.SMALL_CARGO, attributes(0, 0, 100),
        UnitKind.BATTLESHIP, attributes(100, 0, 1_000)
    ), 0, 0, 1_000);

    var outcome = strategy.fight(attackers, defenders, rules, 1);

    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(remaining(outcome, true, 0)).isZero();
    assertThat(remaining(outcome, false, 0)).isEqualTo(1);
    assertThat(stats(outcome, true, 0, 0, UnitKind.SMALL_CARGO).timesFired()).isEqualTo(1);
    assertThat(stats(outcome, false, 0, 0, UnitKind.BATTLESHIP).timesFired()).isEqualTo(1);
    assertThat(stats(outcome, true, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isEqualTo(10.0f);
  }

  @Test
  void recordsShieldOnlyDamageWhenDamageEqualsShield() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 1));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var rules = rules(Map.of(
        UnitKind.BATTLESHIP, attributes(10, 0, 1_000),
        UnitKind.SMALL_CARGO, attributes(0, 10, 1_000)
    ), 0, 0, 1_000);

    var outcome = strategy.fight(attackers, defenders, rules, 1);

    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).shieldDamageTaken()).isEqualTo(10.0f);
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isZero();
    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).shieldDamageDealt()).isEqualTo(10.0f);
    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).hullDamageDealt()).isZero();
  }

  @Test
  void recordsBouncedShieldDamageWhenDamageDoesNotBreakShield() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 1));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var rules = rules(Map.of(
        UnitKind.BATTLESHIP, attributes(4, 0, 1_000),
        UnitKind.SMALL_CARGO, attributes(0, 10, 1_000)
    ), 0, 0, 1_000);

    var outcome = strategy.fight(attackers, defenders, rules, 1);

    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).shieldDamageTaken())
        .isCloseTo(4.0f, offset(0.0001f));
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isZero();
  }

  @Test
  void recordsShieldAndHullDamageWhenDamageBreaksThroughShield() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 1));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var rules = rules(Map.of(
        UnitKind.BATTLESHIP, attributes(15, 0, 1_000),
        UnitKind.SMALL_CARGO, attributes(0, 10, 1_000)
    ), 0, 0, 1_000);

    var outcome = strategy.fight(attackers, defenders, rules, 1);

    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).shieldDamageTaken()).isEqualTo(10.0f);
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isEqualTo(5.0f);
  }

  @Test
  void appliesCombatantTechnologyToDamageShieldAndHull() {
    var lowTechAttackers = List.of(combatant(0, 0, 0, Map.of(UnitKind.BATTLESHIP, 1L)));
    var highTechAttackers = List.of(combatant(10, 0, 0, Map.of(UnitKind.BATTLESHIP, 1L)));
    var lowTechDefenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var highShieldDefenders = List.of(combatant(0, 10, 0, Map.of(UnitKind.SMALL_CARGO, 1L)));
    var highArmorDefenders = List.of(combatant(0, 0, 10, Map.of(UnitKind.SMALL_CARGO, 1L)));
    var rules = rules(Map.of(
        UnitKind.BATTLESHIP, attributes(10, 0, 1_000),
        UnitKind.SMALL_CARGO, attributes(0, 10, 1_000)
    ), 0, 0, 1_000);

    var lowTechOutcome = strategy.fight(lowTechAttackers, lowTechDefenders, rules, 1);
    var highWeaponOutcome = strategy.fight(highTechAttackers, lowTechDefenders, rules, 1);
    var highShieldOutcome = strategy.fight(highTechAttackers, highShieldDefenders, rules, 1);
    var highArmorOutcome = strategy.fight(highTechAttackers, highArmorDefenders, rules, 1);

    assertThat(stats(lowTechOutcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isZero();
    assertThat(stats(highWeaponOutcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isEqualTo(10.0f);
    assertThat(stats(highShieldOutcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isZero();
    assertThat(remaining(highArmorOutcome, false, 0)).isEqualTo(1);
  }

  @Test
  void rapidFireCanStopImmediatelyOrProduceAdditionalShotsDependingOnSeed() {
    var attackers = List.of(combatant(UnitKind.DEATH_STAR, 1));
    var defenders = List.of(combatant(UnitKind.BATTLE_CRUISER, 1));
    var rules = rules(Map.of(
        UnitKind.DEATH_STAR, attributes(0, 0, 1_000, Map.of(UnitKind.BATTLE_CRUISER, 2)),
        UnitKind.BATTLE_CRUISER, attributes(0, 0, 1_000)
    ), 0, 0, 1_000);

    var immediateStop = strategy.fight(attackers, defenders, rules, 1);
    var additionalShot = strategy.fight(attackers, defenders, rules, 12);

    assertThat(stats(immediateStop, true, 0, 0, UnitKind.DEATH_STAR).timesFired()).isEqualTo(1);
    assertThat(stats(additionalShot, true, 0, 0, UnitKind.DEATH_STAR).timesFired()).isGreaterThan(1);
    assertThat(stats(additionalShot, false, 0, 0, UnitKind.BATTLE_CRUISER).timesWasShot())
        .isEqualTo(stats(additionalShot, true, 0, 0, UnitKind.DEATH_STAR).timesFired());
  }
}
