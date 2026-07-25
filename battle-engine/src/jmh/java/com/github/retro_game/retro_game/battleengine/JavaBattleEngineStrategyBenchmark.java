package com.github.retro_game.retro_game.battleengine;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class JavaBattleEngineStrategyBenchmark {
  private final JavaBattleEngineStrategy strategy = new JavaBattleEngineStrategy();

  @Param({"mixed", "rapid-fire"})
  private String scenario;

  private BattleRules rules;
  private List<Combatant> attackers;
  private List<Combatant> defenders;
  private int seed;

  @Setup
  public void setUp() {
    rules = "rapid-fire".equals(scenario) ? rapidFireRules() : mixedRules();
    attackers = attackers();
    defenders = defenders();
    seed = 42;
  }

  @Benchmark
  public void fight(Blackhole blackhole) {
    blackhole.consume(strategy.fight(attackers, defenders, rules, seed++));
  }

  private static List<Combatant> attackers() {
    return List.of(
        combatant(18, 16, 15, Map.of(
            UnitKind.LITTLE_FIGHTER, 4_000L,
            UnitKind.CRUISER, 800L,
            UnitKind.BATTLESHIP, 450L,
            UnitKind.BATTLE_CRUISER, 350L,
            UnitKind.DESTROYER, 80L
        )),
        combatant(12, 10, 11, Map.of(
            UnitKind.SMALL_CARGO, 1_500L,
            UnitKind.HEAVY_FIGHTER, 700L,
            UnitKind.BOMBER, 90L,
            UnitKind.DEATH_STAR, 3L
        ))
    );
  }

  private static List<Combatant> defenders() {
    return List.of(
        combatant(16, 18, 17, Map.of(
            UnitKind.ROCKET_LAUNCHER, 6_000L,
            UnitKind.LIGHT_LASER, 3_500L,
            UnitKind.GAUSS_CANNON, 650L,
            UnitKind.PLASMA_TURRET, 120L,
            UnitKind.SMALL_SHIELD_DOME, 1L,
            UnitKind.LARGE_SHIELD_DOME, 1L
        )),
        combatant(11, 14, 13, Map.of(
            UnitKind.LARGE_CARGO, 1_000L,
            UnitKind.RECYCLER, 500L,
            UnitKind.BATTLE_CRUISER, 200L,
            UnitKind.ION_CANNON, 400L
        ))
    );
  }

  private static Combatant combatant(int weaponsTechnology, int shieldingTechnology, int armorTechnology,
                                     Map<UnitKind, Long> unitGroups) {
    var groups = new EnumMap<UnitKind, Long>(UnitKind.class);
    groups.putAll(unitGroups);
    return new Combatant(1L, new CombatantCoordinates(1, 1, 1, 0), weaponsTechnology, shieldingTechnology,
        armorTechnology, groups);
  }

  private static BattleRules mixedRules() {
    var attrs = new UnitAttributes[UnitKind.values().length];
    for (var kind : UnitKind.values()) {
      attrs[kind.ordinal()] = attributes(100, 100, 1_000, Map.of());
    }

    attrs[UnitKind.LITTLE_FIGHTER.ordinal()] = attributes(50, 10, 400, Map.of());
    attrs[UnitKind.HEAVY_FIGHTER.ordinal()] = attributes(150, 25, 1_000, Map.of());
    attrs[UnitKind.CRUISER.ordinal()] = attributes(400, 50, 2_700,
        Map.of(UnitKind.ROCKET_LAUNCHER, 10, UnitKind.LITTLE_FIGHTER, 3));
    attrs[UnitKind.BATTLESHIP.ordinal()] = attributes(1_000, 200, 6_000, Map.of());
    attrs[UnitKind.BATTLE_CRUISER.ordinal()] = attributes(700, 400, 7_000,
        Map.of(UnitKind.SMALL_CARGO, 3, UnitKind.LARGE_CARGO, 4, UnitKind.BATTLESHIP, 7));
    attrs[UnitKind.BOMBER.ordinal()] = attributes(1_000, 500, 7_500,
        Map.of(UnitKind.ROCKET_LAUNCHER, 20, UnitKind.LIGHT_LASER, 20, UnitKind.ION_CANNON, 10));
    attrs[UnitKind.DESTROYER.ordinal()] = attributes(2_000, 500, 11_000,
        Map.of(UnitKind.BATTLE_CRUISER, 2));
    attrs[UnitKind.DEATH_STAR.ordinal()] = attributes(200_000, 50_000, 900_000,
        Map.of(UnitKind.BATTLE_CRUISER, 15));

    attrs[UnitKind.ROCKET_LAUNCHER.ordinal()] = attributes(80, 20, 200, Map.of());
    attrs[UnitKind.LIGHT_LASER.ordinal()] = attributes(100, 25, 200, Map.of());
    attrs[UnitKind.GAUSS_CANNON.ordinal()] = attributes(1_100, 200, 3_500, Map.of());
    attrs[UnitKind.ION_CANNON.ordinal()] = attributes(150, 500, 800, Map.of());
    attrs[UnitKind.PLASMA_TURRET.ordinal()] = attributes(3_000, 300, 10_000, Map.of());
    attrs[UnitKind.SMALL_SHIELD_DOME.ordinal()] = attributes(1, 2_000, 2_000, Map.of());
    attrs[UnitKind.LARGE_SHIELD_DOME.ordinal()] = attributes(1, 10_000, 10_000, Map.of());
    return new BattleRules(attrs);
  }

  private static BattleRules rapidFireRules() {
    var attrs = new UnitAttributes[UnitKind.values().length];
    for (var kind : UnitKind.values()) {
      var rapidFire = new int[UnitKind.values().length];
      for (var target : UnitKind.values()) {
        rapidFire[target.ordinal()] = target.ordinal() % 3 == 0 ? 2 : 5;
      }
      attrs[kind.ordinal()] = new UnitAttributes(100, 100, 1_000, rapidFire);
    }
    return new BattleRules(attrs);
  }

  private static UnitAttributes attributes(float weapons, float shield, float armor,
                                           Map<UnitKind, Integer> rapidFireAgainst) {
    return new UnitAttributes(weapons, shield, armor, UnitAttributes.makeRapidFire(rapidFireAgainst));
  }
}
