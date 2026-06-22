package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.entity.Resources;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.entity.UnitType;
import com.github.retro_game.retro_game.entity.User;
import com.github.retro_game.retro_game.model.CatalogItem;
import com.github.retro_game.retro_game.service.CatalogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
class UnitServiceImpl implements UnitService {
  private final double hyperspaceTechnologyExpandsCargo;

  UnitServiceImpl(@Value("${retro-game.hyperspace-technology-expands-cargo:0.0}")
                  double hyperspaceTechnologyExpandsCargo) {
    Assert.isTrue(hyperspaceTechnologyExpandsCargo >= 0.0,
        "retro-game.hyperspace-technology-expands-cargo must be at least 0");
    this.hyperspaceTechnologyExpandsCargo = hyperspaceTechnologyExpandsCargo;
  }

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

  @Override
  public long getCapacity(UnitKind kind, User user) {
    long capacity = CatalogItem.of(kind.name()).getCapacity();
    int level = user.getTechnologyLevel(TechnologyKind.HYPERSPACE_TECHNOLOGY);
    return Math.round(capacity * (1.0 + hyperspaceTechnologyExpandsCargo * level));
  }

  @Override
  public long getNumUnitsForCapacity(UnitKind kind, User user, Resources resources) {
    var neededCapacity = resources.getMetal() + resources.getCrystal() + resources.getDeuterium();
    return getNumUnitsForCapacity(kind, user, neededCapacity);
  }

  @Override
  public long getNumUnitsForCapacity(UnitKind kind, User user, double neededCapacity) {
    assert CatalogItem.of(kind.name()).getUnitType() == UnitType.FLEET && kind != UnitKind.SOLAR_SATELLITE;
    var unitCapacity = getCapacity(kind, user);
    assert unitCapacity > 0;
    return (long) Math.ceil(neededCapacity / unitCapacity);
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
