package com.github.retro_game.retro_game.battleengine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnProperty(value = "retro-game.battle-engine", havingValue = "native")
public final class NativeBattleEngineStrategy implements BattleEngineStrategy {
  static {
    System.loadLibrary("BattleEngine");
  }

  private native boolean init(UnitAttributes[] unitsAttributes);

  @Override
  public BattleOutcome fight(List<Combatant> attackers, List<Combatant> defenders, BattleRules rules, int seed) {
    if (!init(rules.unitsAttributes())) {
      throw new IllegalStateException("Failed to init battle engine");
    }
    return fightNative(attackers, defenders, seed);
  }

  private native BattleOutcome fightNative(List<Combatant> attackers, List<Combatant> defenders, int seed);
}
