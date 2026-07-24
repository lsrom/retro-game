package com.github.retro_game.retro_game.battleengine;

public record BattleRules(UnitAttributes[] unitsAttributes) {
  public BattleRules {
    if (unitsAttributes == null) {
      throw new IllegalArgumentException("unitsAttributes cannot be null");
    }
    if (unitsAttributes.length != UnitKind.values().length) {
      throw new IllegalArgumentException("unitsAttributes must have one entry per unit kind");
    }
  }
}
