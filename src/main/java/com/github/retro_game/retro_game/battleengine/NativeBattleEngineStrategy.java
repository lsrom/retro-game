package com.github.retro_game.retro_game.battleengine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

@Component
@ConditionalOnProperty(value = "retro-game.battle-engine", havingValue = "native")
public final class NativeBattleEngineStrategy implements BattleEngineStrategy {
  static {
    System.loadLibrary("BattleEngine");
  }

  public NativeBattleEngineStrategy() {
    // NOTE: as of phase 5, makeUnitsAttributes() reads unit combat stats from
    // the content catalog. The catalog is seeded on ApplicationReadyEvent —
    // after bean construction — so on a fresh database this constructor runs
    // before the catalog exists. The native engine is inactive by default
    // (retro-game.battle-engine=java) and out of scope for this phase; moving
    // this init past catalog seeding belongs with the native-engine rework.
    var unitsAttributes = UnitAttributes.makeUnitsAttributes();
    var success = init(unitsAttributes);
    Assert.isTrue(success, "Failed to init battle engine");
  }

  private native boolean init(UnitAttributes[] unitsAttributes);

  @Override
  public native BattleOutcome fight(List<Combatant> attackers, List<Combatant> defenders, int seed);
}
