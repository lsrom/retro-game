package com.github.retro_game.retro_game.battleengine;

import java.util.Map;

public record UnitAttributes(float weapons, float shield, float armor, int[] rapidFire) {
  public UnitAttributes {
    if (rapidFire == null) {
      throw new IllegalArgumentException("rapidFire cannot be null");
    }
    if (rapidFire.length != UnitKind.values().length) {
      throw new IllegalArgumentException("rapidFire must have one entry per unit kind");
    }
  }

  public static int[] makeRapidFire(Map<UnitKind, Integer> rapidFireAgainst) {
    var rapidFire = new int[UnitKind.values().length];
    for (var entry : rapidFireAgainst.entrySet()) {
      var kind = entry.getKey();
      var n = entry.getValue();
      rapidFire[kind.ordinal()] = n;
    }
    return rapidFire;
  }
}
