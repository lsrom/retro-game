package com.github.retro_game.retro_game.controller;

import com.github.retro_game.retro_game.dto.CoordinatesDto;
import com.github.retro_game.retro_game.dto.CoordinatesKindDto;
import com.github.retro_game.retro_game.dto.EspionageReportDto;
import com.github.retro_game.retro_game.dto.ResourcesDto;
import com.github.retro_game.retro_game.dto.TechnologyKindDto;
import com.github.retro_game.retro_game.dto.UnitKindDto;
import com.github.retro_game.retro_game.service.ReportService;
import com.github.retro_game.retro_game.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ReportsControllerTest {

  @Test
  void websimLinkIncludesAttackerPositionTechnologiesAndShipsButNotDefensesOrSolarSatellites() {
    ReportsController controller = new ReportsController(
        false,
        "https://websim.example/?",
        mock(ReportService.class),
        mock(UserService.class));
    EspionageReportDto report = new EspionageReportDto(
        new Date(0),
        2L,
        "enemy",
        new CoordinatesDto(1, 2, 3, CoordinatesKindDto.PLANET),
        12000,
        0,
        0.0,
        new ResourcesDto(100.0, 200.0, 300.0),
        null,
        null,
        null,
        null);
    Map<UnitKindDto, Integer> attackerUnits = new EnumMap<>(UnitKindDto.class);
    attackerUnits.put(UnitKindDto.SMALL_CARGO, 5);
    attackerUnits.put(UnitKindDto.BATTLE_CRUISER, 7);
    attackerUnits.put(UnitKindDto.SOLAR_SATELLITE, 11);
    attackerUnits.put(UnitKindDto.ROCKET_LAUNCHER, 13);
    attackerUnits.put(UnitKindDto.LARGE_CARGO, 0);
    Map<TechnologyKindDto, Integer> attackerTechnologies = new EnumMap<>(TechnologyKindDto.class);
    attackerTechnologies.put(TechnologyKindDto.WEAPONS_TECHNOLOGY, 12);
    attackerTechnologies.put(TechnologyKindDto.SHIELDING_TECHNOLOGY, 13);
    attackerTechnologies.put(TechnologyKindDto.ARMOR_TECHNOLOGY, 14);

    String link = controller.generateWebsimLink(
        report,
        new ReportsController.WebsimAttackerContext(
            new CoordinatesDto(4, 5, 6, CoordinatesKindDto.PLANET),
            attackerUnits,
            attackerTechnologies));

    assertTrue(link.contains("attacker_pos=4:5:6"));
    assertTrue(link.contains("tech_a0_0=12"));
    assertTrue(link.contains("tech_a0_1=13"));
    assertTrue(link.contains("tech_a0_2=14"));
    assertTrue(link.contains("ship_a0_0_b=5"));
    assertTrue(link.contains("ship_a0_13_b=7"));
    assertFalse(link.contains("ship_a0_10_b=11"));
    assertFalse(link.contains("ship_a0_14_b=13"));
    assertFalse(link.contains("ship_a0_1_b=0"));
  }
}
