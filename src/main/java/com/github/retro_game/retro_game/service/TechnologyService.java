package com.github.retro_game.retro_game.service;

import com.github.retro_game.retro_game.dto.TechnologiesAndQueuePairDto;

public interface TechnologyService {
  TechnologiesAndQueuePairDto getTechnologiesAndQueuePair(long bodyId);

  void research(long bodyId, String kind);

  void moveDown(long bodyId, int sequenceNumber);

  void moveUp(long bodyId, int sequenceNumber);

  void cancel(long bodyId, int sequenceNumber);
}
