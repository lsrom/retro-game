package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.battleengine.CombatantCoordinates;
import com.github.retro_game.retro_game.entity.Coordinates;

public final class BattleEngineCoordinates {
  private BattleEngineCoordinates() {
  }

  public static CombatantCoordinates convert(Coordinates coordinates) {
    return new CombatantCoordinates(
        coordinates.getGalaxy(),
        coordinates.getSystem(),
        coordinates.getPosition(),
        coordinates.getKind().ordinal()
    );
  }
}
