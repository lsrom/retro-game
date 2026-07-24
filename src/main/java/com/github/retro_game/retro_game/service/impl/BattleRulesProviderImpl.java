package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.battleengine.BattleRules;
import com.github.retro_game.retro_game.battleengine.BattleRulesProvider;
import com.github.retro_game.retro_game.battleengine.UnitAttributes;
import com.github.retro_game.retro_game.battleengine.UnitKind;
import com.github.retro_game.retro_game.model.behavior.ItemBehaviorRegistry;
import com.github.retro_game.retro_game.service.CatalogService;
import org.springframework.stereotype.Component;

@Component
public class BattleRulesProviderImpl implements BattleRulesProvider {
  @Override
  public BattleRules getBattleRules() {
    var catalog = CatalogService.getInstance();
    var attrs = new UnitAttributes[UnitKind.values().length];
    for (var kind : UnitKind.values()) {
      var definition = catalog.getDefinition(kind.name());
      var weapons = (float) definition.getWeapons();
      var shield = (float) definition.getShield();
      var armor = (float) definition.getArmor();
      var rapidFire = UnitAttributes.makeRapidFire(ItemBehaviorRegistry.unitBehavior(kind.name()).getRapidFireAgainst());
      attrs[kind.ordinal()] = new UnitAttributes(weapons, shield, armor, rapidFire);
    }
    return new BattleRules(attrs);
  }
}
