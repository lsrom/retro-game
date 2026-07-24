package com.github.retro_game.retro_game.differential;

import com.github.retro_game.retro_game.battleengine.BattleRules;
import com.github.retro_game.retro_game.battleengine.Combatant;
import com.github.retro_game.retro_game.battleengine.CombatantCoordinates;
import com.github.retro_game.retro_game.battleengine.CombatantOutcome;
import com.github.retro_game.retro_game.battleengine.JavaBattleEngineStrategy;
import com.github.retro_game.retro_game.battleengine.NativeBattleEngineStrategy;
import com.github.retro_game.retro_game.battleengine.UnitAttributes;
import com.github.retro_game.retro_game.battleengine.UnitKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SpringBootTest
@EnabledIfSystemProperty(named = "retro-game.native-tests", matches = "true")
public class BattleEngineDifferentialTest {
  private static final long RANDOM_SEED = 42L;
  private static final int NUM_BATTLES = 1000;
  private final JavaBattleEngineStrategy javaBattleEngine = new JavaBattleEngineStrategy();
  private final NativeBattleEngineStrategy nativeBattleEngine = new NativeBattleEngineStrategy();
  private final BattleRules rules = makeRules();

  private static Combatant generateCombatant(Random random) {
    var weaponsTechnology = random.nextInt(30);
    var shieldingTechnology = random.nextInt(30);
    var armorTechnology = random.nextInt(30);
    var unitGroups = new EnumMap<UnitKind, Long>(UnitKind.class);
    var numKinds = random.nextInt(UnitKind.values().length + 1);
    for (var i = 0; i < numKinds; i++) {
      var k = random.nextInt(UnitKind.values().length);
      var kind = UnitKind.values()[k];
      var n = (long) random.nextInt(1000);
      unitGroups.put(kind, n);
    }
    return new Combatant(1, new CombatantCoordinates(1, 1, 1, 0), weaponsTechnology, shieldingTechnology,
        armorTechnology, unitGroups);
  }

  private static BattleRules makeRules() {
    var attrs = new UnitAttributes[UnitKind.values().length];
    for (var kind : UnitKind.values()) {
      attrs[kind.ordinal()] = new UnitAttributes(100, 100, 1000, new int[UnitKind.values().length]);
    }
    attrs[UnitKind.DEATH_STAR.ordinal()].rapidFire()[UnitKind.BATTLE_CRUISER.ordinal()] = 15;
    return new BattleRules(attrs);
  }

  private static List<Combatant> generateCombatants(Random random) {
    var numCombatants = random.nextInt(10);
    var combatants = new ArrayList<Combatant>(numCombatants);
    for (var i = 0; i < numCombatants; i++)
      combatants.add(generateCombatant(random));
    return combatants;
  }

  private void assertOutcomesEqual(List<CombatantOutcome> lhs, List<CombatantOutcome> rhs) {
    Assertions.assertEquals(lhs.size(), rhs.size());
    for (var i = 0; i < lhs.size(); i++) {
      var a = lhs.get(i);
      var b = rhs.get(i);
      Assertions.assertEquals(a.unitGroupsStats(), b.unitGroupsStats());
    }
  }

  @Test
  public void test() {
    long javaTime = 0;
    long nativeTime = 0;

    var random = new Random(RANDOM_SEED);

    for (var i = 0; i < NUM_BATTLES; i++) {
      var seed = random.nextInt();
      var attackers = generateCombatants(random);
      var defenders = generateCombatants(random);

      long t1 = System.nanoTime();
      var javaOutcome = javaBattleEngine.fight(attackers, defenders, rules, seed);
      long t2 = System.nanoTime();
      var nativeOutcome = nativeBattleEngine.fight(attackers, defenders, rules, seed);
      long t3 = System.nanoTime();

      javaTime += t2 - t1;
      nativeTime += t3 - t2;

      assertOutcomesEqual(javaOutcome.attackersOutcomes(), nativeOutcome.attackersOutcomes());
      assertOutcomesEqual(javaOutcome.defendersOutcomes(), nativeOutcome.defendersOutcomes());
    }

    System.out.printf("Battle Engine Perf: java=%,dns native=%,dns\n", javaTime, nativeTime);
  }

  @Test
  public void fixedRegressionSeeds() {
    var seeds = List.of(1, 9, 12, 12345, -12345, Integer.MIN_VALUE, Integer.MAX_VALUE);
    var attackers = List.of(
        combatant(1, 0, 0, Map.of(UnitKind.BATTLESHIP, 3L, UnitKind.DEATH_STAR, 1L)),
        combatant(10, 5, 3, Map.of(UnitKind.CRUISER, 4L)));
    var defenders = List.of(
        combatant(0, 10, 4, Map.of(UnitKind.SMALL_CARGO, 10L, UnitKind.BATTLE_CRUISER, 2L)),
        combatant(2, 1, 0, Map.of(UnitKind.ROCKET_LAUNCHER, 20L)));

    for (var seed : seeds) {
      assertSameOutcome(attackers, defenders, rules, seed);
    }
  }

  @Test
  public void randomizedBattlesWhereEitherSideMayBeEmpty() {
    var random = new Random(7);

    for (var i = 0; i < 200; i++) {
      assertSameOutcome(generateCombatants(random), generateCombatants(random), rules, random.nextInt());
    }
  }

  @Test
  public void randomizedBattlesWithHighTechnologyValues() {
    var random = new Random(99);

    for (var i = 0; i < 200; i++) {
      assertSameOutcome(generateHighTechCombatants(random), generateHighTechCombatants(random), rules, random.nextInt());
    }
  }

  @Test
  public void randomizedBattlesWithRapidFireHeavyRules() {
    var random = new Random(123);
    var rapidFireRules = makeRapidFireHeavyRules();

    for (var i = 0; i < 200; i++) {
      assertSameOutcome(generateCombatants(random), generateCombatants(random), rapidFireRules, random.nextInt());
    }
  }

  private void assertSameOutcome(List<Combatant> attackers, List<Combatant> defenders, BattleRules rules, int seed) {
    var javaOutcome = javaBattleEngine.fight(attackers, defenders, rules, seed);
    var nativeOutcome = nativeBattleEngine.fight(attackers, defenders, rules, seed);

    Assertions.assertEquals(javaOutcome.seed(), nativeOutcome.seed());
    Assertions.assertEquals(javaOutcome.numRounds(), nativeOutcome.numRounds());
    assertOutcomesEqual(javaOutcome.attackersOutcomes(), nativeOutcome.attackersOutcomes());
    assertOutcomesEqual(javaOutcome.defendersOutcomes(), nativeOutcome.defendersOutcomes());
  }

  private static Combatant combatant(int weaponsTechnology, int shieldingTechnology, int armorTechnology,
                                     Map<UnitKind, Long> unitGroups) {
    var groups = new EnumMap<UnitKind, Long>(UnitKind.class);
    groups.putAll(unitGroups);
    return new Combatant(1, new CombatantCoordinates(1, 1, 1, 0), weaponsTechnology, shieldingTechnology,
        armorTechnology, groups);
  }

  private static List<Combatant> generateHighTechCombatants(Random random) {
    var numCombatants = random.nextInt(10);
    var combatants = new ArrayList<Combatant>(numCombatants);
    for (var i = 0; i < numCombatants; i++) {
      var weaponsTechnology = random.nextInt(500);
      var shieldingTechnology = random.nextInt(500);
      var armorTechnology = random.nextInt(500);
      var unitGroups = new EnumMap<UnitKind, Long>(UnitKind.class);
      var numKinds = random.nextInt(UnitKind.values().length + 1);
      for (var j = 0; j < numKinds; j++) {
        unitGroups.put(UnitKind.values()[random.nextInt(UnitKind.values().length)], (long) random.nextInt(100));
      }
      combatants.add(new Combatant(1, new CombatantCoordinates(1, 1, 1, 0), weaponsTechnology,
          shieldingTechnology, armorTechnology, unitGroups));
    }
    return combatants;
  }

  private static BattleRules makeRapidFireHeavyRules() {
    var attrs = new UnitAttributes[UnitKind.values().length];
    for (var kind : UnitKind.values()) {
      var rapidFire = new int[UnitKind.values().length];
      for (var target : UnitKind.values()) {
        rapidFire[target.ordinal()] = target.ordinal() % 4 == 0 ? 2 : 5;
      }
      attrs[kind.ordinal()] = new UnitAttributes(100, 100, 1000, rapidFire);
    }
    return new BattleRules(attrs);
  }
}
