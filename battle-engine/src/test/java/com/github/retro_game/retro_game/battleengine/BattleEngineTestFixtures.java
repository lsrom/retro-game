package com.github.retro_game.retro_game.battleengine;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class BattleEngineTestFixtures {
  static final CombatantCoordinates COORDINATES = new CombatantCoordinates(1, 1, 1, 0);

  private BattleEngineTestFixtures() {
  }

  static BattleRules rules(float weapons, float shield, float armor) {
    var attrs = new UnitAttributes[UnitKind.values().length];
    Arrays.fill(attrs, attributes(weapons, shield, armor));
    return new BattleRules(attrs);
  }

  static BattleRules rules(Map<UnitKind, UnitAttributes> overrides, float weapons, float shield, float armor) {
    var attrs = new UnitAttributes[UnitKind.values().length];
    Arrays.fill(attrs, attributes(weapons, shield, armor));
    for (var entry : overrides.entrySet()) {
      attrs[entry.getKey().ordinal()] = entry.getValue();
    }
    return new BattleRules(attrs);
  }

  static UnitAttributes attributes(float weapons, float shield, float armor) {
    return new UnitAttributes(weapons, shield, armor, noRapidFire());
  }

  static UnitAttributes attributes(float weapons, float shield, float armor, Map<UnitKind, Integer> rapidFireAgainst) {
    return new UnitAttributes(weapons, shield, armor, UnitAttributes.makeRapidFire(rapidFireAgainst));
  }

  static int[] noRapidFire() {
    return new int[UnitKind.values().length];
  }

  static Combatant combatant(UnitKind kind, long count) {
    return combatant(0, 0, 0, Map.of(kind, count));
  }

  static Combatant combatant(int weaponsTechnology, int shieldingTechnology, int armorTechnology,
                             Map<UnitKind, Long> unitGroups) {
    return new Combatant(1, COORDINATES, weaponsTechnology, shieldingTechnology, armorTechnology, groups(unitGroups));
  }

  static EnumMap<UnitKind, Long> groups(Map<UnitKind, Long> unitGroups) {
    var groups = new EnumMap<UnitKind, Long>(UnitKind.class);
    groups.putAll(unitGroups);
    return groups;
  }

  static UnitGroupStats stats(BattleOutcome outcome, boolean attackers, int combatantIdx, int round, UnitKind kind) {
    var outcomes = attackers ? outcome.attackersOutcomes() : outcome.defendersOutcomes();
    return outcomes.get(combatantIdx).unitGroupsStats().get(round).get(kind);
  }

  static long remaining(BattleOutcome outcome, boolean attackers, int round) {
    var outcomes = attackers ? outcome.attackersOutcomes() : outcome.defendersOutcomes();
    return outcomes.stream()
        .mapToLong(o -> o.unitGroupsStats().get(round).values().stream()
            .mapToLong(UnitGroupStats::numRemainingUnits)
            .sum())
        .sum();
  }

  static double totalShieldDamageDealt(List<CombatantOutcome> outcomes, int round) {
    return outcomes.stream()
        .flatMap(o -> o.unitGroupsStats().get(round).values().stream())
        .mapToDouble(UnitGroupStats::shieldDamageDealt)
        .sum();
  }

  static double totalShieldDamageTaken(List<CombatantOutcome> outcomes, int round) {
    return outcomes.stream()
        .flatMap(o -> o.unitGroupsStats().get(round).values().stream())
        .mapToDouble(UnitGroupStats::shieldDamageTaken)
        .sum();
  }

  static double totalHullDamageDealt(List<CombatantOutcome> outcomes, int round) {
    return outcomes.stream()
        .flatMap(o -> o.unitGroupsStats().get(round).values().stream())
        .mapToDouble(UnitGroupStats::hullDamageDealt)
        .sum();
  }

  static double totalHullDamageTaken(List<CombatantOutcome> outcomes, int round) {
    return outcomes.stream()
        .flatMap(o -> o.unitGroupsStats().get(round).values().stream())
        .mapToDouble(UnitGroupStats::hullDamageTaken)
        .sum();
  }
}
