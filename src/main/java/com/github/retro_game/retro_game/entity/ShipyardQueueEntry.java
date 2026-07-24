package com.github.retro_game.retro_game.entity;

import com.github.retro_game.retro_game.battleengine.UnitKind;

public record ShipyardQueueEntry(UnitKind kind, int count) {
}
