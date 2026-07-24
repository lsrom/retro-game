package com.github.retro_game.retro_game.battleengine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.combatant;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.rules;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class BattleEngineImplTest {
  private final BattleEngineStrategy strategy = mock(BattleEngineStrategy.class);
  private final BattleRulesProvider rulesProvider = mock(BattleRulesProvider.class);
  private final BattleEngineImpl engine = new BattleEngineImpl(strategy, rulesProvider);

  @Test
  void returnsSingleRoundOutcomesWithoutDelegatingWhenAttackersHaveNoUnits() {
    var attackers = List.of(combatant(UnitKind.SMALL_CARGO, 0));
    var defenders = List.of(combatant(UnitKind.BATTLESHIP, 2));

    var outcome = engine.fight(attackers, defenders, 123);

    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(outcome.attackersOutcomes()).hasSize(1);
    assertThat(outcome.defendersOutcomes()).hasSize(1);
    assertThat(outcome.attackersOutcomes().getFirst().unitGroupsStats()).hasSize(1);
    assertThat(outcome.defendersOutcomes().getFirst().unitGroupsStats()).hasSize(1);
    assertThat(outcome.attackersOutcomes().getFirst().unitGroupsStats().getFirst())
        .containsOnlyKeys(UnitKind.values());
    assertThat(outcome.defendersOutcomes().getFirst().unitGroupsStats().getFirst())
        .containsOnlyKeys(UnitKind.values());
    assertThat(outcome.attackersOutcomes().getFirst().unitGroupsStats().getFirst().get(UnitKind.SMALL_CARGO)
        .numRemainingUnits()).isZero();
    assertThat(outcome.defendersOutcomes().getFirst().unitGroupsStats().getFirst().get(UnitKind.BATTLESHIP)
        .numRemainingUnits()).isEqualTo(2);
    assertAllStatsExceptRemainingAreZero(outcome.attackersOutcomes());
    assertAllStatsExceptRemainingAreZero(outcome.defendersOutcomes());
    verifyNoInteractions(strategy, rulesProvider);
  }

  @Test
  void returnsSingleRoundDefenderOutcomesWithoutDelegatingWhenAttackersListIsEmpty() {
    var defenders = List.of(combatant(UnitKind.BATTLESHIP, 2), combatant(UnitKind.SMALL_CARGO, 1));

    var outcome = engine.fight(List.of(), defenders, 123);

    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(outcome.attackersOutcomes()).isEmpty();
    assertThat(outcome.defendersOutcomes()).hasSize(defenders.size());
    assertThat(outcome.defendersOutcomes()).allSatisfy(defenderOutcome -> {
      assertThat(defenderOutcome.unitGroupsStats()).hasSize(1);
      assertThat(defenderOutcome.unitGroupsStats().getFirst()).containsOnlyKeys(UnitKind.values());
    });
    assertAllStatsExceptRemainingAreZero(outcome.defendersOutcomes());
    verifyNoInteractions(strategy, rulesProvider);
  }

  @Test
  void returnsSingleRoundOutcomesWithoutDelegatingWhenDefendersHaveNoUnits() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 3));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 0));

    var outcome = engine.fight(attackers, defenders, 123);

    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(outcome.attackersOutcomes()).hasSize(1);
    assertThat(outcome.defendersOutcomes()).hasSize(1);
    assertThat(outcome.attackersOutcomes().getFirst().unitGroupsStats().getFirst().get(UnitKind.BATTLESHIP)
        .numRemainingUnits()).isEqualTo(3);
    assertThat(outcome.defendersOutcomes().getFirst().unitGroupsStats().getFirst().get(UnitKind.SMALL_CARGO)
        .numRemainingUnits()).isZero();
    assertAllStatsExceptRemainingAreZero(outcome.attackersOutcomes());
    assertAllStatsExceptRemainingAreZero(outcome.defendersOutcomes());
    verifyNoInteractions(strategy, rulesProvider);
  }

  @Test
  void returnsSingleRoundAttackerOutcomesWithoutDelegatingWhenDefendersListIsEmpty() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, 3), combatant(UnitKind.CRUISER, 2));

    var outcome = engine.fight(attackers, List.of(), 123);

    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(outcome.attackersOutcomes()).hasSize(attackers.size());
    assertThat(outcome.defendersOutcomes()).isEmpty();
    assertThat(outcome.attackersOutcomes()).allSatisfy(attackerOutcome -> {
      assertThat(attackerOutcome.unitGroupsStats()).hasSize(1);
      assertThat(attackerOutcome.unitGroupsStats().getFirst()).containsOnlyKeys(UnitKind.values());
    });
    assertAllStatsExceptRemainingAreZero(outcome.attackersOutcomes());
    verifyNoInteractions(strategy, rulesProvider);
  }

  @Test
  void returnsEmptyOutcomesWithoutDelegatingWhenBothSidesAreEmpty() {
    var outcome = engine.fight(List.of(), List.of(), 123);

    assertThat(outcome.seed()).isEqualTo(123);
    assertThat(outcome.numRounds()).isEqualTo(1);
    assertThat(outcome.attackersOutcomes()).isEmpty();
    assertThat(outcome.defendersOutcomes()).isEmpty();
    verifyNoInteractions(strategy, rulesProvider);
  }

  @Test
  void delegatesToStrategyWhenBothSidesHaveUnits() {
    var attackers = List.of(combatant(UnitKind.LITTLE_FIGHTER, 1));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var rules = rules(1, 2, 3);
    var delegatedOutcome = new BattleOutcome(99, 4, List.of(new CombatantOutcome(List.of())),
        List.of(new CombatantOutcome(List.of())));
    when(rulesProvider.getBattleRules()).thenReturn(rules);
    when(strategy.fight(attackers, defenders, rules, 99)).thenReturn(delegatedOutcome);

    var outcome = engine.fight(attackers, defenders, 99);

    assertThat(outcome).isSameAs(delegatedOutcome);
    verify(rulesProvider).getBattleRules();
    verify(strategy).fight(attackers, defenders, rules, 99);
  }

  @Test
  void shortcutHandlesSparseUnitGroupsByAddingZeroStatsForMissingKinds() {
    var attackers = List.of(BattleEngineTestFixtures.combatant(0, 0, 0,
        Map.of(UnitKind.BATTLESHIP, 1L, UnitKind.CRUISER, 0L)));

    var outcome = engine.fight(attackers, List.of(), 123);

    var roundStats = outcome.attackersOutcomes().getFirst().unitGroupsStats().getFirst();
    assertThat(roundStats).containsOnlyKeys(UnitKind.values());
    assertThat(roundStats.get(UnitKind.BATTLESHIP).numRemainingUnits()).isEqualTo(1);
    assertThat(roundStats.get(UnitKind.CRUISER).numRemainingUnits()).isZero();
    assertThat(roundStats.get(UnitKind.DEATH_STAR).numRemainingUnits()).isZero();
    verifyNoInteractions(strategy, rulesProvider);
  }

  @Test
  void shortcutRejectsNegativeUnitCountsWithAssertionsEnabled() {
    var attackers = List.of(BattleEngineTestFixtures.combatant(0, 0, 0,
        Map.of(UnitKind.BATTLESHIP, -1L)));

    assertThatThrownBy(() -> engine.fight(attackers, List.of(), 123))
        .isInstanceOf(AssertionError.class);

    verifyNoInteractions(strategy, rulesProvider);
  }

  private static void assertAllStatsExceptRemainingAreZero(List<CombatantOutcome> outcomes) {
    for (var outcome : outcomes) {
      for (var round : outcome.unitGroupsStats()) {
        for (var stats : round.values()) {
          assertThat(stats.timesFired()).isZero();
          assertThat(stats.timesWasShot()).isZero();
          assertThat(stats.shieldDamageDealt()).isZero();
          assertThat(stats.hullDamageDealt()).isZero();
          assertThat(stats.shieldDamageTaken()).isZero();
          assertThat(stats.hullDamageTaken()).isZero();
        }
      }
    }
  }
}
