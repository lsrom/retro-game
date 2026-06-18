package com.github.retro_game.retro_game.model;

import com.github.retro_game.retro_game.entity.BuildingKind;
import com.github.retro_game.retro_game.entity.ItemDefinition;
import com.github.retro_game.retro_game.entity.Resources;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.service.CatalogService;

// A helper for cost & required energy calculation.
//
// The base values (cost, cost factor, required energy) are read from the
// content catalog via CatalogService, so they can be tuned through the admin
// panel; the growth formulas below are unchanged.
public class ItemCostUtils {
  public static Resources getCost(BuildingKind kind, int level) {
    // When a building of level 1 needs to be destroyed, the passed level is 0.
    assert level >= 0;
    var definition = CatalogService.getInstance().getDefinition(kind.name());
    var cost = baseCost(definition);
    cost.mul(Math.pow(definition.getCostFactor(), level - 1));
    cost.floor();
    return cost;
  }

  public static Resources getCost(TechnologyKind kind, int level) {
    assert level >= 1;
    var definition = CatalogService.getInstance().getDefinition(kind.name());
    // This is the formula for Astrophysics, but can be applied to other technologies as well.
    var cost = baseCost(definition);
    cost.mul(0.01);
    cost.mul(Math.pow(definition.getCostFactor(), level - 1));
    cost.add(new Resources(0.5, 0.5, 0.5));
    cost.floor();
    cost.mul(100.0);
    cost.floor();
    return cost;
  }

  public static Resources getCost(UnitKind kind) {
    // Units have a flat cost: no level, no cost factor.
    return baseCost(CatalogService.getInstance().getDefinition(kind.name()));
  }

  public static int getRequiredEnergy(BuildingKind kind, int level) {
    assert level >= 0;
    var definition = CatalogService.getInstance().getDefinition(kind.name());
    return getRequiredEnergy(definition.getBaseEnergy(), definition.getCostFactor(), level);
  }

  public static int getRequiredEnergy(TechnologyKind kind, int level) {
    assert level >= 1;
    var definition = CatalogService.getInstance().getDefinition(kind.name());
    return getRequiredEnergy(definition.getBaseEnergy(), definition.getCostFactor(), level);
  }

  // Builds a fresh, mutable Resources from a catalog definition's base cost.
  private static Resources baseCost(ItemDefinition definition) {
    return new Resources(definition.getMetalCost(), definition.getCrystalCost(), definition.getDeuteriumCost());
  }

  private static int getRequiredEnergy(int base, double factor, int level) {
    assert level >= 0;
    return (int) (base * Math.pow(factor, level - 1));
  }
}
