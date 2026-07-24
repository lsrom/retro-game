package com.github.retro_game.retro_game.battleengine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.combatant;
import static com.github.retro_game.retro_game.battleengine.BattleEngineTestFixtures.rules;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaBattleEngineStrategyValidationTest {
  private final JavaBattleEngineStrategy strategy = new JavaBattleEngineStrategy();

  @Test
  void acceptsValidMinimalBattle() {
    var attackers = List.of(combatant(UnitKind.SMALL_CARGO, 1));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));

    assertThatCode(() -> {
      var outcome = strategy.fight(attackers, defenders, rules(0, 0, 100), 1);

      assertThat(outcome.numRounds()).isBetween(1, 6);
      assertThat(outcome.attackersOutcomes()).hasSize(1);
      assertThat(outcome.defendersOutcomes()).hasSize(1);
      assertThat(outcome.attackersOutcomes().getFirst().unitGroupsStats()).hasSize(outcome.numRounds());
      assertThat(outcome.defendersOutcomes().getFirst().unitGroupsStats()).hasSize(outcome.numRounds());
    }).doesNotThrowAnyException();
  }

  @Test
  void returnsZeroRoundsAndEmptyRoundStatsWhenEitherSideIsEmpty() {
    var attacker = combatant(UnitKind.BATTLESHIP, 1);
    var defender = combatant(UnitKind.SMALL_CARGO, 1);
    var rules = rules(10, 0, 1_000);

    var noAttackers = strategy.fight(List.of(), List.of(defender), rules, 1);
    var noDefenders = strategy.fight(List.of(attacker), List.of(), rules, 1);
    var noCombatants = strategy.fight(List.of(), List.of(), rules, 1);

    assertThat(noAttackers.numRounds()).isZero();
    assertThat(noAttackers.attackersOutcomes()).isEmpty();
    assertThat(noAttackers.defendersOutcomes()).hasSize(1);
    assertThat(noAttackers.defendersOutcomes().getFirst().unitGroupsStats()).isEmpty();
    assertThat(noDefenders.numRounds()).isZero();
    assertThat(noDefenders.attackersOutcomes()).hasSize(1);
    assertThat(noDefenders.defendersOutcomes()).isEmpty();
    assertThat(noDefenders.attackersOutcomes().getFirst().unitGroupsStats()).isEmpty();
    assertThat(noCombatants.numRounds()).isZero();
    assertThat(noCombatants.attackersOutcomes()).isEmpty();
    assertThat(noCombatants.defendersOutcomes()).isEmpty();
  }

  @Test
  void rejectsTotalUnitCountsOverIntegerMaxValue() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, (long) Integer.MAX_VALUE + 1L));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));

    assertThatThrownBy(() -> strategy.fight(attackers, defenders, rules(1, 1, 1), 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Too many units");
  }

  @Test
  void rejectsCombatantCountsOverByteMaxValueWithAssertionsEnabled() {
    var attackers = new ArrayList<Combatant>();
    for (var i = 0; i <= Byte.MAX_VALUE; i++) {
      attackers.add(combatant(UnitKind.BATTLESHIP, 0));
    }
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));

    assertThatThrownBy(() -> strategy.fight(attackers, defenders, rules(1, 1, 1), 1))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void characterizesNegativeUnitCountPassedDirectlyToStrategy() {
    var attackers = List.of(combatant(UnitKind.BATTLESHIP, -1));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));

    assertThatThrownBy(() -> strategy.fight(attackers, defenders, rules(1, 1, 1), 1))
        .isInstanceOf(NegativeArraySizeException.class);
  }

  @Test
  void characterizesNullInputs() {
    var combatant = combatant(UnitKind.BATTLESHIP, 1);
    var rules = rules(1, 1, 1);

    assertThatThrownBy(() -> strategy.fight(null, List.of(combatant), rules, 1))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> strategy.fight(List.of(combatant), null, rules, 1))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> strategy.fight(List.of((Combatant) null), List.of(combatant), rules, 1))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> strategy.fight(List.of(new Combatant(1, BattleEngineTestFixtures.COORDINATES,
        0, 0, 0, null)), List.of(combatant), rules, 1))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void characterizesNullUnitCounts() {
    var groups = new EnumMap<UnitKind, Long>(UnitKind.class);
    groups.put(UnitKind.BATTLESHIP, null);
    var attackers = List.of(new Combatant(1, BattleEngineTestFixtures.COORDINATES, 0, 0, 0, groups));
    var defenders = List.of(combatant(UnitKind.SMALL_CARGO, 1));

    assertThatThrownBy(() -> strategy.fight(attackers, defenders, rules(1, 1, 1), 1))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void unitKindCountStillFitsByteBackedStorage() {
    assertThat(UnitKind.values().length).isLessThanOrEqualTo(Byte.MAX_VALUE);
  }
}
