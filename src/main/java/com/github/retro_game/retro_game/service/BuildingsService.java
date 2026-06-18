package com.github.retro_game.retro_game.service;

import com.github.retro_game.retro_game.dto.BuildingsAndQueuePairDto;

public interface BuildingsService {
  BuildingsAndQueuePairDto getBuildingsAndQueuePair(long bodyId);

  void construct(long bodyId, String kind);

  void destroy(long bodyId, String kind);

  void moveDown(long bodyId, int sequenceNumber);

  void moveUp(long bodyId, int sequenceNumber);

  void cancel(long bodyId, int sequenceNumber);
}
