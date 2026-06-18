package com.github.retro_game.retro_game.dto;

import java.util.EnumMap;

public record RepeatFleetDto(EnumMap<UnitKindDto, Integer> units, CoordinatesDto coordinates) {
}
