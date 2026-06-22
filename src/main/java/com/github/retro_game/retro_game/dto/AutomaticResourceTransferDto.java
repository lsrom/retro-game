package com.github.retro_game.retro_game.dto;

import java.util.Date;

public record AutomaticResourceTransferDto(
    long id,
    long targetBodyId,
    String targetBodyName,
    CoordinatesDto targetCoordinates,
    boolean enabled,
    UnitKindDto shipKind,
    int shipCount,
    ResourcesDto resources,
    int speedFactor,
    int runHour,
    int runMinute,
    Date nextRunAt,
    Date lastRunAt,
    String lastError
) {
}
