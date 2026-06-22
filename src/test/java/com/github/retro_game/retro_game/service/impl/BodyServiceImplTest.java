package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.entity.Body;
import com.github.retro_game.retro_game.entity.BuildingKind;
import com.github.retro_game.retro_game.entity.Coordinates;
import com.github.retro_game.retro_game.entity.CoordinatesKind;
import com.github.retro_game.retro_game.entity.ProductionFactors;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.User;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BodyServiceImplTest {
  @Test
  public void plasmaProductionBonusDoesNotOverflowAtHighSpeed() {
    var user = new User();
    user.setTechnologyLevel(TechnologyKind.PLASMA_TECHNOLOGY, 1);

    var body = new Body();
    body.setUser(user);
    body.setCoordinates(new Coordinates(1, 1, 1, CoordinatesKind.PLANET));
    body.setProductionFactors(new ProductionFactors());
    body.setTemperature(0);
    body.setBuildingLevel(BuildingKind.METAL_MINE, 60);
    body.setBuildingLevel(BuildingKind.SOLAR_PLANT, 100);

    var service = new BodyServiceImpl(null, null,
        60_000,
        20, 10, 0,
        30, 20, 10,
        10, 10, 20,
        20, 30, 10,
        0.05,
        6, 3, 100,
        true,
        null, null, null, null, null);

    var production = service.getProduction(body);

    assertThat(production.plasmaMetalBonus()).isPositive();
    assertThat(production.metalProduction())
        .isEqualTo(production.metalBaseProduction() + production.metalMineProduction() + production.plasmaMetalBonus());
    assertThat(production.metalProduction()).isGreaterThan(production.metalBaseProduction() + production.metalMineProduction());
  }
}
