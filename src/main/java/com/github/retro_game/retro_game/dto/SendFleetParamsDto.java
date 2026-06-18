package com.github.retro_game.retro_game.dto;

import org.springframework.lang.Nullable;

import java.util.Map;

public class SendFleetParamsDto {
  private final long bodyId;
  private final Map<UnitKindDto, Integer> units;
  private final MissionDto mission;
  private final Integer holdTime;
  private final CoordinatesDto coordinates;
  private final int factor;
  private final ResourcesDto resources;
  private final Long partyId;
  private final boolean exactUnits;

  public SendFleetParamsDto(long bodyId, Map<UnitKindDto, Integer> units, MissionDto mission,
                            @Nullable Integer holdTime, CoordinatesDto coordinates, int factor, ResourcesDto resources,
                            @Nullable Long partyId) {
    this(bodyId, units, mission, holdTime, coordinates, factor, resources, partyId, false);
  }

  public SendFleetParamsDto(long bodyId, Map<UnitKindDto, Integer> units, MissionDto mission,
                            @Nullable Integer holdTime, CoordinatesDto coordinates, int factor, ResourcesDto resources,
                            @Nullable Long partyId, boolean exactUnits) {
    this.bodyId = bodyId;
    this.units = units;
    this.mission = mission;
    this.holdTime = holdTime;
    this.coordinates = coordinates;
    this.factor = factor;
    this.resources = resources;
    this.partyId = partyId;
    this.exactUnits = exactUnits;
  }

  public long getBodyId() {
    return bodyId;
  }

  public Map<UnitKindDto, Integer> getUnits() {
    return units;
  }

  public MissionDto getMission() {
    return mission;
  }

  public Integer getHoldTime() {
    return holdTime;
  }

  public CoordinatesDto getCoordinates() {
    return coordinates;
  }

  public int getFactor() {
    return factor;
  }

  public ResourcesDto getResources() {
    return resources;
  }

  public Long getPartyId() {
    return partyId;
  }

  public boolean isExactUnits() {
    return exactUnits;
  }
}
