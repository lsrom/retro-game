package com.github.retro_game.retro_game.service;

import org.springframework.lang.Nullable;

public interface TechnologyDescriptionService {
  @Nullable
  String getExtraDescription(String kind, int currentLevel);
}
