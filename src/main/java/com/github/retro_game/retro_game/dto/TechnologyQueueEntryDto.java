package com.github.retro_game.retro_game.dto;

import java.util.Date;

public record TechnologyQueueEntryDto(String kind, int sequence, int level, ResourcesDto cost,
                                      int requiredEnergy, long bodyId, int effectiveLabLevel, Date finishAt,
                                      boolean downMovable, boolean upMovable, boolean cancelable) {
}
