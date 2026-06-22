package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.entity.Resources;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.entity.User;

public interface UnitService {
  int getSpeed(UnitKind kind, User user);

  long getCapacity(UnitKind kind, User user);

  long getNumUnitsForCapacity(UnitKind kind, User user, Resources resources);

  long getNumUnitsForCapacity(UnitKind kind, User user, double neededCapacity);

  double getWeapons(UnitKind kind, User user);

  double getShield(UnitKind kind, User user);

  double getArmor(UnitKind kind, User user);
}
