package com.github.retro_game.retro_game.battleengine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.remaining;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.rules;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.totalHullDamageDealt;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.totalHullDamageTaken;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.totalShieldDamageDealt;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.totalShieldDamageTaken;
import static org.assertj.core.api.Assertions.assertThat;

class JavaBattleEngineStrategyInvariantTest {
  private static final int NUM_BATTLES = 200;
  private final JavaBattleEngineStrategy strategy = new JavaBattleEngineStrategy();

  @Test
  void randomizedBattlesPreserveCoreOutcomeInvariants() {
    var random = new Random(42);
    var rules = rules(Map.of(
        UnitKind.DEATH_STAR, BattleEngineTestFixtures.attributes(40, 10, 1_000,
            Map.of(UnitKind.BATTLE_CRUISER, 2)),
        UnitKind.BATTLE_CRUISER, BattleEngineTestFixtures.attributes(30, 10, 1_000)
    ), 20, 10, 1_000);

    for (var i = 0; i < NUM_BATTLES; i++) {
      var attackers = combatants(random);
      var defenders = combatants(random);
      var outcome = strategy.fight(attackers, defenders, rules, random.nextInt());

      assertThat(outcome.numRounds()).isBetween(0, 6);
      assertOutcomesHaveExpectedShape(outcome.attackersOutcomes(), attackers.size(), outcome.numRounds());
      assertOutcomesHaveExpectedShape(outcome.defendersOutcomes(), defenders.size(), outcome.numRounds());
      assertRemainingUnitsNeverIncrease(outcome, true);
      assertRemainingUnitsNeverIncrease(outcome, false);
      assertStatsAreNonNegative(outcome.attackersOutcomes());
      assertStatsAreNonNegative(outcome.defendersOutcomes());
      assertDamageIsConservedBetweenSides(outcome);
    }
  }

  private static List<Combatant> combatants(Random random) {
    var combatants = new ArrayList<Combatant>();
    var numCombatants = random.nextInt(4);
    for (var i = 0; i < numCombatants; i++) {
      var groups = new EnumMap<UnitKind, Long>(UnitKind.class);
      for (var kind : UnitKind.values()) {
        if (random.nextInt(5) == 0) {
          groups.put(kind, (long) random.nextInt(4));
        }
      }
      combatants.add(new Combatant(i + 1, BattleEngineTestFixtures.COORDINATES, random.nextInt(4),
          random.nextInt(4), random.nextInt(4), groups));
    }
    return combatants;
  }

  private static void assertOutcomesHaveExpectedShape(List<CombatantOutcome> outcomes, int numCombatants,
                                                      int numRounds) {
    assertThat(outcomes).hasSize(numCombatants);
    for (var outcome : outcomes) {
      assertThat(outcome.unitGroupsStats()).hasSize(numRounds);
      for (var round : outcome.unitGroupsStats()) {
        assertThat(round).containsOnlyKeys(UnitKind.values());
      }
    }
  }

  private static void assertRemainingUnitsNeverIncrease(BattleOutcome outcome, boolean attackers) {
    var previous = Long.MAX_VALUE;
    for (var round = 0; round < outcome.numRounds(); round++) {
      var current = remaining(outcome, attackers, round);
      assertThat(current).isLessThanOrEqualTo(previous);
      previous = current;
    }
  }

  private static void assertStatsAreNonNegative(List<CombatantOutcome> outcomes) {
    for (var outcome : outcomes) {
      for (var round : outcome.unitGroupsStats()) {
        for (var stats : round.values()) {
          assertThat(stats.numRemainingUnits()).isGreaterThanOrEqualTo(0);
          assertThat(stats.timesFired()).isGreaterThanOrEqualTo(0);
          assertThat(stats.timesWasShot()).isGreaterThanOrEqualTo(0);
          assertThat(stats.shieldDamageDealt()).isGreaterThanOrEqualTo(0.0f);
          assertThat(stats.hullDamageDealt()).isGreaterThanOrEqualTo(0.0f);
          assertThat(stats.shieldDamageTaken()).isGreaterThanOrEqualTo(0.0f);
          assertThat(stats.hullDamageTaken()).isGreaterThanOrEqualTo(0.0f);
        }
      }
    }
  }

  private static void assertDamageIsConservedBetweenSides(BattleOutcome outcome) {
    for (var round = 0; round < outcome.numRounds(); round++) {
      assertThat(totalShieldDamageDealt(outcome.attackersOutcomes(), round))
          .isCloseTo(totalShieldDamageTaken(outcome.defendersOutcomes(), round), withinFloatTolerance());
      assertThat(totalShieldDamageDealt(outcome.defendersOutcomes(), round))
          .isCloseTo(totalShieldDamageTaken(outcome.attackersOutcomes(), round), withinFloatTolerance());
      assertThat(totalHullDamageDealt(outcome.attackersOutcomes(), round))
          .isCloseTo(totalHullDamageTaken(outcome.defendersOutcomes(), round), withinFloatTolerance());
      assertThat(totalHullDamageDealt(outcome.defendersOutcomes(), round))
          .isCloseTo(totalHullDamageTaken(outcome.attackersOutcomes(), round), withinFloatTolerance());
    }
  }

  private static org.assertj.core.data.Offset<Double> withinFloatTolerance() {
    return org.assertj.core.data.Offset.offset(0.001);
  }
}
