package com.github.retro_game.retro_game.dto;

import org.springframework.lang.Nullable;

import java.util.Date;

public record UnitDto(String kind, int currentCount, int futureCount, ResourcesDto cost, long buildingTime,
                      ResourcesDto missingResources, long neededSmallCargoes, long neededLargeCargoes,
                      @Nullable Date accumulationTime, int maxBuildable) {
}
