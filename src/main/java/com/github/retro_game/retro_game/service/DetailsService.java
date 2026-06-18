package com.github.retro_game.retro_game.service;

import com.github.retro_game.retro_game.dto.*;

public interface DetailsService {
  BuildingDetailsDto getBuildingDetails(long bodyId, String kind);

  TechnologyDetailsDto getTechnologyDetails(long bodyId, String kind);

  UnitDetailsDto getUnitDetails(long bodyId, String kind);
}
