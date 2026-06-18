package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.entity.User;
import com.github.retro_game.retro_game.model.CatalogItem;
import com.github.retro_game.retro_game.service.CatalogService;
import org.springframework.stereotype.Service;

@Service
class UnitServiceImpl implements UnitService {
  @Override
  public int getSpeed(UnitKind kind, User user) {
    // Propulsion (base speed and drive) is code-only behavior; the catalog item
    // supplies it through the kind-keyed behavior registry.
    CatalogItem item = CatalogItem.of(kind.name());

    int speed = item.getBaseSpeed(user);
    if (speed == 0) {
      return 0;
    }

    TechnologyKind drive = item.getDrive(user);
    if (drive == null) {
      return 0;
    }
    int level = user.getTechnologyLevel(drive);

    assert speed % 10 == 0;
    switch (drive) {
      case COMBUSTION_DRIVE:
        return speed + speed / 10 * level;
      case IMPULSE_DRIVE:
        return speed + speed / 10 * 2 * level;
      case HYPERSPACE_DRIVE:
        return speed + speed / 10 * 3 * level;
    }
    return 0;
  }

  // The base weapons, shield and armor come from the editable content catalog;
  // the weapons/shielding/armor technology bonus is then applied on top.
  @Override
  public double getWeapons(UnitKind kind, User user) {
    return CatalogService.getInstance().getDefinition(kind.name()).getWeapons() *
        (1.0 + 0.1 * user.getTechnologyLevel(TechnologyKind.WEAPONS_TECHNOLOGY));
  }

  @Override
  public double getShield(UnitKind kind, User user) {
    return CatalogService.getInstance().getDefinition(kind.name()).getShield() *
        (1.0 + 0.1 * user.getTechnologyLevel(TechnologyKind.SHIELDING_TECHNOLOGY));
  }

  @Override
  public double getArmor(UnitKind kind, User user) {
    return CatalogService.getInstance().getDefinition(kind.name()).getArmor() *
        (1.0 + 0.1 * user.getTechnologyLevel(TechnologyKind.ARMOR_TECHNOLOGY));
  }
}
