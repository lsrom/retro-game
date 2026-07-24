package com.github.retro_game.retro_game.battleengine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.attributes;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.combatant;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.remaining;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.rules;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.stats;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class JavaBattleEngineStrategyBehaviorTest {
  private final JavaBattleEngineStrategy strategy = new JavaBattleEngineStrategy();

  @Test
  void stopsAfterAttackersAreEliminated() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        rules(Map.of(
            UnitKind.SMALL_CARGO, attributes(0, 0, 100),
            UnitKind.BATTLESHIP, attributes(100, 0, 1_000)
        ), 0, 0, 1_000),
        1);

    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(remaining(outcome, true, 0)).isZero();
    assertThat(remaining(outcome, false, 0)).isEqualTo(1);
  }

  @Test
  void stopsAfterDefendersAreEliminated() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        rules(Map.of(
            UnitKind.BATTLESHIP, attributes(100, 0, 1_000),
            UnitKind.SMALL_CARGO, attributes(0, 0, 100)
        ), 0, 0, 1_000),
        1);

    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(remaining(outcome, true, 0)).isEqualTo(1);
    assertThat(remaining(outcome, false, 0)).isZero();
  }

  @Test
  void bothSidesCanDestroyEachOtherInSameRound() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        rules(100, 0, 100),
        1);

    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(remaining(outcome, true, 0)).isZero();
    assertThat(remaining(outcome, false, 0)).isZero();
  }

  @Test
  void killedUnitsAreAbsentFromRemainingStatsForTheirFinalRound() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        rules(100, 0, 100),
        1);

    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).numRemainingUnits()).isZero();
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).numRemainingUnits()).isZero();
  }

  @Test
  void defenderKilledBeforeCleanupStillFiresInCurrentImplementation() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        rules(100, 0, 100),
        1);

    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).timesFired()).isEqualTo(1);
    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).timesWasShot()).isEqualTo(1);
  }

  @Test
  void combatantsAndKindsAreTrackedIndependentlyAndInputOrderIsPreserved() {
    var firstAttackerGroups = BattleEngineTestFixtures.groups(Map.of(
        UnitKind.BATTLESHIP, 2L,
        UnitKind.CRUISER, 1L));
    var secondAttackerGroups = BattleEngineTestFixtures.groups(Map.of(UnitKind.BATTLESHIP, 3L));
    var attackers = List.of(
        new Combatant(10, BattleEngineTestFixtures.COORDINATES, 0, 0, 0, firstAttackerGroups),
        new Combatant(20, BattleEngineTestFixtures.COORDINATES, 0, 0, 0, secondAttackerGroups));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));

    var outcome = strategy.fight(attackers, defenders, rules(0, 0, 1_000), 1);

    assertThat(outcome.attackersOutcomes()).hasSize(2);
    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).numRemainingUnits()).isEqualTo(2);
    assertThat(stats(outcome, true, 0, 0, UnitKind.CRUISER).numRemainingUnits()).isEqualTo(1);
    assertThat(stats(outcome, true, 1, 0, UnitKind.BATTLESHIP).numRemainingUnits()).isEqualTo(3);
    assertThat(stats(outcome, true, 1, 0, UnitKind.CRUISER).numRemainingUnits()).isZero();
  }

  @Test
  void combatantWithNoUnitsGetsZeroStatsForEveryRoundWhenSideHasOtherAliveCombatants() {
    var attackers = List.of(
        combatant(UnitKind.BATTLESHIP, 1),
        new Combatant(2, BattleEngineTestFixtures.COORDINATES, 0, 0, 0, new EnumMap<>(UnitKind.class)));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));

    var outcome = strategy.fight(attackers, defenders, rules(0, 0, 1_000), 1);

    assertThat(outcome.numRounds()).isEqualTo(6);
    for (var round = 0; round < outcome.numRounds(); round++) {
      for (var kind : UnitKind.values()) {
        assertZeroStats(stats(outcome, true, 1, round, kind));
      }
    }
  }

  @Test
  void explicitZeroCountGroupCreatesNoUnitsAndMissingKindsRemainZero() {
    var attackers = List.of(combatant(0, 0, 0, Map.of(UnitKind.BATTLESHIP, 0L, UnitKind.CRUISER, 1L)));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));

    var outcome = strategy.fight(attackers, defenders, rules(0, 0, 1_000), 1);

    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).numRemainingUnits()).isZero();
    assertThat(stats(outcome, true, 0, 0, UnitKind.CRUISER).numRemainingUnits()).isEqualTo(1);
    assertThat(stats(outcome, true, 0, 0, UnitKind.DEATH_STAR).numRemainingUnits()).isZero();
  }

  @Test
  void largeManageableUnitCountAggregatesSurvivorsWithoutOverflow() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 1_500));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));

    var outcome = strategy.fight(attackers, defenders, rules(0, 0, 1_000), 1);

    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).numRemainingUnits()).isEqualTo(1_500);
  }

  @Test
  void enumMapInsertionOrderDoesNotAffectDeterministicOutput() {
    var first = new EnumMap<UnitKind, Long>(UnitKind.class);
    first.put(UnitKind.BATTLESHIP, 1L);
    first.put(UnitKind.CRUISER, 1L);
    var second = new EnumMap<UnitKind, Long>(UnitKind.class);
    second.put(UnitKind.CRUISER, 1L);
    second.put(UnitKind.BATTLESHIP, 1L);
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 2));

    var firstOutcome = strategy.fight(List.of(new Combatant(1, BattleEngineTestFixtures.COORDINATES, 0, 0, 0, first)),
        defenders, rules(10, 0, 1_000), 123);
    var secondOutcome = strategy.fight(List.of(new Combatant(1, BattleEngineTestFixtures.COORDINATES, 0, 0, 0, second)),
        defenders, rules(10, 0, 1_000), 123);

    assertThat(secondOutcome).isEqualTo(firstOutcome);
  }

  @Test
  void shieldsAreRestoredAtTheStartOfEachRound() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        rules(Map.of(
            UnitKind.BATTLESHIP, attributes(4, 0, 1_000),
            UnitKind.SMALL_CARGO, attributes(0, 10, 1_000)
        ), 0, 0, 1_000),
        1);

    assertThat(outcome.numRounds()).isEqualTo(6);
    for (var round = 0; round < outcome.numRounds(); round++) {
      assertThat(stats(outcome, false, 0, round, UnitKind.SMALL_CARGO).shieldDamageTaken())
          .isCloseTo(4.0f, offset(0.0001f));
      assertThat(stats(outcome, false, 0, round, UnitKind.SMALL_CARGO).hullDamageTaken()).isZero();
    }
  }

  @Test
  void shieldingTechnologyChangesRestoredShieldAndDamageSplit() {
    var attacker = List.of(combatant(UnitKind.BATTLESHIP, 1));
    var lowShield = List.of(combatant(0, 0, 0, Map.of(UnitKind.SMALL_CARGO, 1L)));
    var highShield = List.of(combatant(0, 10, 0, Map.of(UnitKind.SMALL_CARGO, 1L)));
    var rules = rules(Map.of(
        UnitKind.BATTLESHIP, attributes(15, 0, 1_000),
        UnitKind.SMALL_CARGO, attributes(0, 10, 1_000)
    ), 0, 0, 1_000);

    var lowShieldOutcome = strategy.fight(attacker, lowShield, rules, 1);
    var highShieldOutcome = strategy.fight(attacker, highShield, rules, 1);

    assertThat(stats(lowShieldOutcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isEqualTo(5.0f);
    assertThat(stats(highShieldOutcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isZero();
    assertThat(stats(highShieldOutcome, false, 0, 0, UnitKind.SMALL_CARGO).shieldDamageTaken()).isEqualTo(15.0f);
  }

  @Test
  void shieldRestorationAppliesToBothSidesIndependently() {
    var attackers = List.of(combatant(0, 10, 0, Map.of(UnitKind.BATTLESHIP, 1L)));
    var defenders = List.of(combatant(0, 0, 0, Map.of(UnitKind.SMALL_CARGO, 1L)));

    var outcome = strategy.fight(attackers, defenders, rules(15, 10, 1_000), 1);

    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).hullDamageTaken()).isZero();
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isEqualTo(5.0f);
  }

  @Test
  void damageIsCappedAtRemainingHullAndExplosionDoesNotAddHullDamage() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        rules(Map.of(
            UnitKind.BATTLESHIP, attributes(100, 0, 1_000),
            UnitKind.SMALL_CARGO, attributes(0, 0, 100)
        ), 0, 0, 1_000),
        1);

    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isEqualTo(10.0f);
    assertThat(remaining(outcome, false, 0)).isZero();
  }

  @Test
  void damageLessThanShieldCanRoundDownToZeroShieldDamage() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        rules(Map.of(
            UnitKind.BATTLESHIP, attributes(0.05f, 0, 1_000),
            UnitKind.SMALL_CARGO, attributes(0, 10, 1_000)
        ), 0, 0, 1_000),
        1);

    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).shieldDamageTaken()).isZero();
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isZero();
  }

  @Test
  void fractionalDamageIsRecordedWithFloatPrecision() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        rules(Map.of(
            UnitKind.BATTLESHIP, attributes(12.5f, 0, 1_000),
            UnitKind.SMALL_CARGO, attributes(0, 7.25f, 1_000)
        ), 0, 0, 1_000),
        1);

    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).shieldDamageTaken())
        .isCloseTo(7.25f, offset(0.0001f));
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken())
        .isCloseTo(5.25f, offset(0.0001f));
  }

  @Test
  void targetAtExactlySeventyPercentHullDoesNotExplode() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        rules(Map.of(
            UnitKind.BATTLESHIP, attributes(30, 0, 1_000),
            UnitKind.SMALL_CARGO, attributes(0, 0, 1_000)
        ), 0, 0, 1_000),
        9);

    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isEqualTo(30.0f);
    assertThat(remaining(outcome, false, 0)).isEqualTo(1);
  }

  @Test
  void damagedTargetBelowSeventyPercentHullMaySurviveOrExplodeDependingOnSeed() {
    var rules = rules(Map.of(
        UnitKind.BATTLESHIP, attributes(31, 0, 1_000),
        UnitKind.SMALL_CARGO, attributes(0, 0, 1_000)
    ), 0, 0, 1_000);
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 1));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));

    var survives = strategy.fight(attackers, defenders, rules, 1);
    var explodes = strategy.fight(attackers, defenders, rules, 9);

    assertThat(remaining(survives, false, 0)).isEqualTo(1);
    assertThat(remaining(explodes, false, 0)).isZero();
    assertThat(stats(explodes, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isEqualTo(31.0f);
  }

  @Test
  void rapidFireValueZeroAndOneOnlyFireOnce() {
    var attackers = List.of(combatant(UnitKind.DEATH_STAR, 1));
    var defenders = List.of(combatant(UnitKind.BATTLE_CRUISER, 1));
    var zeroRapidFire = rules(Map.of(
        UnitKind.DEATH_STAR, attributes(0, 0, 1_000),
        UnitKind.BATTLE_CRUISER, attributes(0, 0, 1_000)
    ), 0, 0, 1_000);
    var oneRapidFire = rules(Map.of(
        UnitKind.DEATH_STAR, attributes(0, 0, 1_000, Map.of(UnitKind.BATTLE_CRUISER, 1)),
        UnitKind.BATTLE_CRUISER, attributes(0, 0, 1_000)
    ), 0, 0, 1_000);

    assertThat(stats(strategy.fight(attackers, defenders, zeroRapidFire, 12), true, 0, 0, UnitKind.DEATH_STAR)
        .timesFired()).isEqualTo(1);
    assertThat(stats(strategy.fight(attackers, defenders, oneRapidFire, 12), true, 0, 0, UnitKind.DEATH_STAR)
        .timesFired()).isEqualTo(1);
  }

  @Test
  void rapidFireUsesTargetKindTableAndCanShootDeadTargetBeforeCleanup() {
    var attackers = List.of(combatant(UnitKind.DEATH_STAR, 1));
    var defenders = List.of(combatant(UnitKind.BATTLE_CRUISER, 1));
    var rules = rules(Map.of(
        UnitKind.DEATH_STAR, attributes(100, 0, 1_000, Map.of(UnitKind.BATTLE_CRUISER, 2)),
        UnitKind.BATTLE_CRUISER, attributes(0, 0, 100)
    ), 0, 0, 1_000);

    var outcome = strategy.fight(attackers, defenders, rules, 12);

    assertThat(stats(outcome, true, 0, 0, UnitKind.DEATH_STAR).timesFired()).isGreaterThan(1);
    assertThat(stats(outcome, false, 0, 0, UnitKind.BATTLE_CRUISER).timesWasShot()).isGreaterThan(1);
    assertThat(stats(outcome, false, 0, 0, UnitKind.BATTLE_CRUISER).hullDamageTaken()).isEqualTo(10.0f);
    assertThat(remaining(outcome, false, 0)).isZero();
  }

  @Test
  void rapidFireCanTargetMultipleDefendersAndEventuallyStops() {
    var attackers = List.of(combatant(UnitKind.DEATH_STAR, 1));
    var defenders = List.of(combatant(0, 0, 0, Map.of(UnitKind.BATTLE_CRUISER, 2L)));
    var rules = rules(Map.of(
        UnitKind.DEATH_STAR, attributes(0, 0, 1_000, Map.of(UnitKind.BATTLE_CRUISER, 2)),
        UnitKind.BATTLE_CRUISER, attributes(0, 0, 1_000)
    ), 0, 0, 1_000);

    var outcome = strategy.fight(attackers, defenders, rules, 12);

    assertThat(outcome.numRounds()).isEqualTo(6);
    assertThat(stats(outcome, true, 0, 0, UnitKind.DEATH_STAR).timesFired()).isBetween(2L, 100L);
    assertThat(stats(outcome, false, 0, 0, UnitKind.BATTLE_CRUISER).timesWasShot())
        .isEqualTo(stats(outcome, true, 0, 0, UnitKind.DEATH_STAR).timesFired());
  }

  @Test
  void differentShooterKindsUseDifferentRapidFireTables() {
    var defenders = List.of(combatant(UnitKind.BATTLE_CRUISER, 1));
    var rules = rules(Map.of(
        UnitKind.DEATH_STAR, attributes(0, 0, 1_000, Map.of(UnitKind.BATTLE_CRUISER, 2)),
        UnitKind.BATTLESHIP, attributes(0, 0, 1_000),
        UnitKind.BATTLE_CRUISER, attributes(0, 0, 1_000)
    ), 0, 0, 1_000);

    var deathStarOutcome = strategy.fight(List.of(combatant(UnitKind.DEATH_STAR, 1)), defenders, rules, 12);
    var battleshipOutcome = strategy.fight(List.of(combatant(UnitKind.BATTLESHIP, 1)), defenders, rules, 12);

    assertThat(stats(deathStarOutcome, true, 0, 0, UnitKind.DEATH_STAR).timesFired()).isGreaterThan(1);
    assertThat(stats(battleshipOutcome, true, 0, 0, UnitKind.BATTLESHIP).timesFired()).isEqualTo(1);
  }

  @Test
  void swappingSidesChangesOutcomeWhenFirstStrikeStatsMatter() {
    var rules = rules(Map.of(
        UnitKind.BATTLESHIP, attributes(100, 0, 1_000),
        UnitKind.SMALL_CARGO, attributes(0, 0, 100)
    ), 0, 0, 1_000);

    var battleshipAttacks = strategy.fight(List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)), rules, 1);
    var cargoAttacks = strategy.fight(List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        List.of(combatant(UnitKind.BATTLESHIP, 1)), rules, 1);

    assertThat(remaining(battleshipAttacks, true, 0)).isEqualTo(1);
    assertThat(remaining(battleshipAttacks, false, 0)).isZero();
    assertThat(remaining(cargoAttacks, true, 0)).isZero();
    assertThat(remaining(cargoAttacks, false, 0)).isEqualTo(1);
  }

  @Test
  void negativeTechnologiesAreCharacterizedByCurrentDamageFormulas() {
    var defender = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var baseline = strategy.fight(List.of(combatant(0, 0, 0, Map.of(UnitKind.BATTLESHIP, 1L))),
        defender, rules(10, 0, 1_000), 1);
    var negativeWeapons = strategy.fight(List.of(combatant(-10, 0, 0, Map.of(UnitKind.BATTLESHIP, 1L))),
        defender, rules(10, 0, 1_000), 1);
    var negativeArmorDefender = strategy.fight(List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(0, 0, -10, Map.of(UnitKind.SMALL_CARGO, 1L))), rules(0, 0, 1_000), 1);

    assertThat(stats(baseline, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isEqualTo(10.0f);
    assertThat(stats(negativeWeapons, false, 0, 0, UnitKind.SMALL_CARGO).hullDamageTaken()).isZero();
    assertThat(remaining(negativeArmorDefender, false, 0)).isZero();
  }

  @Test
  void differentTechnologiesOnSameSideApplyPerOwningCombatant() {
    var attackers = List.of(
        combatant(0, 0, 0, Map.of(UnitKind.BATTLESHIP, 1L)),
        combatant(10, 0, 0, Map.of(UnitKind.BATTLESHIP, 1L)));
    var defenders = List.of(combatant(0, 0, 0, Map.of(UnitKind.SMALL_CARGO, 2L)));

    var outcome = strategy.fight(attackers, defenders, rules(10, 0, 1_000), 1);

    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).hullDamageDealt()).isEqualTo(10.0f);
    assertThat(stats(outcome, true, 1, 0, UnitKind.BATTLESHIP).hullDamageDealt()).isEqualTo(20.0f);
  }

  @Test
  void repeatedCopiedInputsAndPositiveSeedsAreDeterministic() {
    var expected = strategy.fight(copiedCombatants(), copiedCombatants(), rules(10, 10, 1_000), Integer.MAX_VALUE);
    for (var i = 0; i < 100; i++) {
      assertThat(strategy.fight(copiedCombatants(), copiedCombatants(), rules(10, 10, 1_000), Integer.MAX_VALUE))
          .isEqualTo(expected);
    }
  }

  @Test
  void differentPositiveSeedsCanProduceDifferentOutcomesWhenRandomnessMatters() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 1));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var rules = rules(Map.of(
        UnitKind.BATTLESHIP, attributes(31, 0, 1_000),
        UnitKind.SMALL_CARGO, attributes(0, 0, 1_000)
    ), 0, 0, 1_000);

    assertThat(strategy.fight(attackers, defenders, rules, 1))
        .isNotEqualTo(strategy.fight(attackers, defenders, rules, 9));
  }

  @Test
  void zeroArmorAndZeroWeaponsUnitsStillFireButAreRemovedAfterRound() {
    var outcome = strategy.fight(
        List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)),
        rules(0, 0, 0),
        1);

    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(stats(outcome, true, 0, 0, UnitKind.BATTLESHIP).timesFired()).isEqualTo(1);
    assertThat(stats(outcome, false, 0, 0, UnitKind.SMALL_CARGO).timesFired()).isEqualTo(1);
    assertThat(remaining(outcome, true, 0)).isZero();
    assertThat(remaining(outcome, false, 0)).isZero();
  }

  @Test
  void verySmallAndVeryLargeAttributesDoNotProduceNegativeStats() {
    var rules = rules(Map.of(
        UnitKind.BATTLESHIP, attributes(Float.MIN_NORMAL, 0, Float.MIN_NORMAL),
        UnitKind.SMALL_CARGO, attributes(1.0e20f, 1.0e20f, 1.0e20f)
    ), 0, 0, 1_000);

    var outcome = strategy.fight(List.of(combatant(UnitKind.BATTLESHIP, 1)),
        List.of(combatant(UnitKind.SMALL_CARGO, 1)), rules, Integer.MAX_VALUE);

    for (var combatantOutcome : allOutcomes(outcome)) {
      for (var round : combatantOutcome.unitGroupsStats()) {
        for (var unitStats : round.values()) {
          assertThat(unitStats.numRemainingUnits()).isGreaterThanOrEqualTo(0);
          assertThat(unitStats.timesFired()).isGreaterThanOrEqualTo(0);
          assertThat(unitStats.timesWasShot()).isGreaterThanOrEqualTo(0);
          assertThat(unitStats.shieldDamageDealt()).isGreaterThanOrEqualTo(0.0f);
          assertThat(unitStats.hullDamageDealt()).isGreaterThanOrEqualTo(0.0f);
          assertThat(unitStats.shieldDamageTaken()).isGreaterThanOrEqualTo(0.0f);
          assertThat(unitStats.hullDamageTaken()).isGreaterThanOrEqualTo(0.0f);
        }
      }
    }
  }

  private static List<Combatant> copiedCombatants() {
    var groups = new EnumMap<UnitKind, Long>(UnitKind.class);
    groups.put(UnitKind.BATTLESHIP, 1L);
    groups.put(UnitKind.CRUISER, 1L);
    return List.of(new Combatant(1, BattleEngineTestFixtures.COORDINATES, 0, 0, 0, groups));
  }

  private static List<CombatantOutcome> allOutcomes(BattleOutcome outcome) {
    var outcomes = new ArrayList<CombatantOutcome>();
    outcomes.addAll(outcome.attackersOutcomes());
    outcomes.addAll(outcome.defendersOutcomes());
    return outcomes;
  }

  private static void assertZeroStats(UnitGroupStats stats) {
    assertThat(stats.numRemainingUnits()).isZero();
    assertThat(stats.timesFired()).isZero();
    assertThat(stats.timesWasShot()).isZero();
    assertThat(stats.shieldDamageDealt()).isZero();
    assertThat(stats.hullDamageDealt()).isZero();
    assertThat(stats.shieldDamageTaken()).isZero();
    assertThat(stats.hullDamageTaken()).isZero();
  }
}
